package ee.ria.EstEIDUtility;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.Signature;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ee.ria.EstEIDUtility.R.id;
import ee.ria.EstEIDUtility.SMInterface.Connected;
import ee.ria.EstEIDUtility.Token.CertListener;
import ee.ria.EstEIDUtility.Token.CertType;
import ee.ria.EstEIDUtility.Token.PinType;
import ee.ria.EstEIDUtility.Token.SignListener;

public class MainActivity extends Activity  {
	TextView content, contentmID;
	private SMInterface sminterface = null;
	byte[] signCert, authCert, signedBytes;
	EditText dialogPinPuk;
	EstEIDToken eidToken;
	MobileIDToken midToken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		content = (TextView) findViewById(R.id.content);
		contentmID = (TextView) findViewById(R.id.content_mID);
		enableButtons(false);
		findViewById(id.button_verify).setEnabled(false);
		findViewById(R.id.button_read_cert_mID).setEnabled(false);
		findViewById(R.id.button_sign_mID).setEnabled(false);
		findViewById(R.id.button_read_personal_mID).setEnabled(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (sminterface != null) {
			sminterface.close();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.action_settings:
				settingsDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void verify(View view){
		EditText textToSign = (EditText)findViewById(R.id.edit_text);
		try{
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(Util.getX509Certificate(signCert).getPublicKey());
			sig.update(textToSign.getText().toString().getBytes());
			content.setText("Signature verify: " + sig.verify(signedBytes));
		}catch (Exception e){
			e.printStackTrace();
			content.setText("Signature verify: failed\n" + e.getMessage());
		}
	}

	public void displayPersonalData(View view){
		try {
			content.setText("Personal data:\n" + eidToken.readPersonalFile());
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void displayCertInfo(View view){
		try {
			switch (view.getId()) {
				case R.id.button_read_cert_ID:
					eidToken.setCertListener(new CertListener() {
						@Override
						public void onCertificateResponse(CertType type, byte[] cert) {
							switch (type) {
								case CertAuth:
									contentmID.setText("Cert common name: " + Util.getCommonName(cert));
									break;
								case CertSign:
									content.setText("Cert common name: " + Util.getCommonName(cert));
									signCert = cert;
									break;
							}
						}

						@Override
						public void onCertificateError(String msg) {
							new AlertDialog.Builder(MainActivity.this)
							.setTitle(R.string.cert_read_failed)
							.setMessage(msg)
							.setNegativeButton("Close", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							}).show();
						}
					});
					eidToken.readCert(EstEIDToken.CertType.CertSign);
					break;
				case R.id.button_read_cert_mID:
					midToken.setCertListener(new CertListener() {
						@Override
						public void onCertificateResponse(CertType type, byte[] cert) {
							switch (type) {
								case CertAuth:
									contentmID.setText("Cert common name: " + Util.getCommonName(cert));
									authCert = cert;
									break;
								case CertSign:
									content.setText("Cert common name: " + Util.getCommonName(cert));
									break;
							}
						}

						@Override
						public void onCertificateError(String msg) {
							new AlertDialog.Builder(MainActivity.this)
							.setTitle(R.string.cert_read_failed)
							.setMessage(msg)
							.setNegativeButton("Close", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							}).show();
						}
					});
					midToken.readCert(EstEIDToken.CertType.CertAuth);
					break;
			}
			findViewById(R.id.button_verify).setEnabled(signCert != null && signedBytes != null);
		} catch (Exception e) {
			switch (view.getId()) {
				case R.id.button_read_cert_ID:
					content.setText(e.getMessage());
					break;
				case R.id.button_read_cert_mID:
					contentmID.setText(e.getMessage());
					break;
			}
			e.printStackTrace();
		}
	}

	public void enableButtons( boolean enable) {
		findViewById(R.id.button_read_personal).setEnabled(enable);
		findViewById(R.id.button_read_cert_ID).setEnabled(enable);
		findViewById(R.id.edit_text).setEnabled(enable);
		findViewById(R.id.button_sign).setEnabled(enable);
		findViewById(R.id.button_auth).setEnabled(enable);
		findViewById(R.id.button_change_puk).setEnabled(enable);
		findViewById(R.id.button_change_pin1).setEnabled(enable);
		findViewById(R.id.button_change_pin2).setEnabled(enable);
		findViewById(R.id.button_unblock_pin1).setEnabled(enable);
		findViewById(R.id.button_unblock_pin2).setEnabled(enable);
	}

	public void connectReader(View view) {
		sminterface = SMInterface.getInstance(this, SMInterface.ACS);
		if (sminterface == null) {
			content.setText("No readers connected");
			return;
		}

		sminterface.connect(new Connected() {
			@Override
			public void connected() {
				enableButtons(sminterface != null);
			}
		});

		eidToken = new EstEIDToken(sminterface, this);
	}

	public void changePin(View view) {
		EditText currentPinPuk = (EditText)findViewById(id.insert_pin);
		EditText newPinPuk = (EditText)findViewById(id.new_pin_puk);
		try {
			PinType type = null;
			switch (view.getId()) {
				case R.id.button_change_pin1: type = PinType.PIN1; break;
				case R.id.button_change_pin2: type = PinType.PIN2; break;
				case R.id.button_change_puk: type = PinType.PUK; break;
			}
			boolean status = eidToken.changePin(currentPinPuk.getText().toString().getBytes(),
					newPinPuk.getText().toString().getBytes(),
					type);
			content.setText(status ? type.name() + " change success" : type.name() + " change failed");
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void unblockPin(View view) {
		EditText currentPinPuk = (EditText)findViewById(R.id.insert_pin);
		try {
			PinType type = null;
			switch (view.getId()) {
				case R.id.button_unblock_pin1: type = PinType.PIN1; break;
				case R.id.button_unblock_pin2: type = PinType.PIN2; break;
			}
			boolean status = eidToken.unblockPin(currentPinPuk.getText().toString().getBytes(), type);
			content.setText(status ? type.name() + " unblock success" : type.name() + " unblock failed");
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void createDialog(final View view) {
		eidToken.setSignListener(new SignListener() {
			@Override
			public void onSignResponse(byte[] signature) {
				signedBytes = signature;
				content.setText(Util.toHex(signature));
				findViewById(R.id.button_verify).setEnabled(signCert != null && signature != null);
			}

			@Override
			public void onSignError(String msg) {
				content.setText(msg);
			}
		});

		if (view.getId() == R.id.button_sign) {
			try {
				EditText textToSign = (EditText)findViewById(R.id.edit_text);
				byte[] textDigest = MessageDigest.getInstance("SHA-1").digest(
						textToSign.getText().toString().getBytes());
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				outputStream.write(new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14}); // SHA1 OID
				outputStream.write(textDigest);
				eidToken.sign(PinType.PIN2, outputStream.toByteArray());
			} catch(Exception e) {
				e.printStackTrace();
				content.setText(e.getMessage());
			}
		} else {
			eidToken.sign(PinType.PIN1,
				new byte[] {0x3F, 0x4B ,(byte) 0xE6 ,0x4B ,(byte) 0xC9 ,0x06 ,0x6F ,0x14 ,(byte) 0x8A ,0x39 ,0x21 ,(byte) 0xD8 ,0x7C ,(byte) 0x94 ,0x41 ,0x40 ,(byte) 0x99 ,0x72 ,0x4B ,0x58 ,0x75 ,(byte) 0xA1 ,0x15 ,0x78 });
		}
	}

	public void settingsDialog() {
		final EditText mobNr = new EditText(this);
		mobNr.setInputType(InputType.TYPE_CLASS_PHONE);
		mobNr.setHint("+372 123456");
		mobNr.setText(getSharedPreferences("Settings", 0).getString("mobileNR", ""));
		final EditText idCode = new EditText(this);
		idCode.setInputType(InputType.TYPE_CLASS_NUMBER);
		idCode.setHint("4710101027");
		idCode.setText(getSharedPreferences("Settings", 0).getString("personalCode", ""));
		idCode.setFilters(new InputFilter[] {new InputFilter.LengthFilter(11)});
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(mobNr);
		layout.addView(idCode);

		final AlertDialog dialog = new AlertDialog.Builder(this)
			.setView(layout)
			.setTitle("Insert phone nr. and ID-code")
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					SharedPreferences.Editor editor = MainActivity.this.getSharedPreferences("Settings", 0).edit();
					editor.putString("mobileNR", mobNr.getText().toString());
					editor.putString("personalCode", idCode.getText().toString());
					editor.commit();
					try {
						midToken = new MobileIDToken(MainActivity.this);
					} catch (Exception e) {
						e.printStackTrace();
					}
					findViewById(R.id.button_read_cert_mID).setEnabled(true);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.show();

		dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(idCode.length() == 11);

		idCode.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(s.length() == 11);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
	}
}
