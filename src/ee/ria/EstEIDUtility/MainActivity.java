package ee.ria.EstEIDUtility;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.Signature;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import ee.ria.EstEIDUtility.EstEIDUtil.PinType;
import ee.ria.EstEIDUtility.R.id;
import ee.ria.EstEIDUtility.SMInterface.Connected;

public class MainActivity extends Activity {
	private TextView content;
	private SMInterface sminterface = null;
	byte[] signCert, signedBytes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		content = (TextView) findViewById(R.id.content);
		enableButtons(false);
		findViewById(id.button_verify).setEnabled(false);
		sminterface = SMInterface.getInstance(this, SMInterface.ACS);
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
		int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}


	public void loginBtnPin1(View view) {
		EditText currentPinPuk = (EditText)findViewById(id.insert_pin);
		try {
			boolean status = EstEIDUtil.login(PinType.PIN1, currentPinPuk.getText().toString().getBytes(), sminterface);
			content.setText(status ? "PIN1 Login success" : "PIN1 Login failed");
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void loginBtnPin2(View view) {
		EditText currentPinPuk = (EditText)findViewById(id.insert_pin);
		try {
			boolean status = EstEIDUtil.login(PinType.PIN2, currentPinPuk.getText().toString().getBytes(), sminterface);
			content.setText(status ? "PIN2 Login success" : "PIN2 Login failed");
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void signText(View view){
		EditText textToSign = (EditText)findViewById(id.edit_text);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			byte[] textDigest = MessageDigest.getInstance("SHA-1").digest(
					textToSign.getText().toString().getBytes());
			outputStream.write(new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14}); // SHA1 OID
			outputStream.write(textDigest);
			signedBytes = EstEIDUtil.sign(outputStream.toByteArray(), PinType.PIN2,  sminterface);
			content.setText(Util.toHex(signedBytes));
			findViewById(id.button_verify).setEnabled(signCert != null && signedBytes != null);
		} catch (Exception e) {
			content.setText("Sign failed" + e.getMessage());
			e.printStackTrace();
		}
	}

	public void internalAuth(View view){
		try {
			byte[] TLSchallenge = new byte[] {0x3F, 0x4B ,(byte) 0xE6 ,0x4B ,(byte) 0xC9 ,0x06 ,0x6F ,0x14 ,(byte) 0x8A ,0x39 ,0x21 ,(byte) 0xD8 ,0x7C ,(byte) 0x94 ,0x41 ,0x40 ,(byte) 0x99 ,0x72 ,0x4B ,0x58 ,0x75 ,(byte) 0xA1 ,0x15 ,0x78 };
			byte[] TLSchallengeBytes = EstEIDUtil.sign(TLSchallenge, PinType.PIN1,  sminterface);
			content.setText(Util.toHex(TLSchallengeBytes));
		} catch (Exception e) {
			content.setText("Auth. failed" + e.getMessage());
			e.printStackTrace();
		}
	}

	public void verify(View view){
		EditText textToSign = (EditText)findViewById(id.edit_text);
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
			content.setText("Personal data:\n" + EstEIDUtil.readPersonalFile(sminterface));
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void displayCertInfo(View view){
		try {
			signCert = EstEIDUtil.readCert(EstEIDUtil.CertType.CertSign, sminterface);
			content.setText("Cert common name: " + Util.getCommonName(signCert));
			findViewById(id.button_verify).setEnabled(signCert != null && signedBytes != null);
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void enableButtons( boolean enable) {
		findViewById(id.button_read_personal).setEnabled(enable);
		findViewById(id.button_read_cert).setEnabled(enable);
		findViewById(id.insert_pin).setEnabled(enable);
		findViewById(id.button_login_pin1).setEnabled(enable);
		findViewById(id.button_login_pin2).setEnabled(enable);
		findViewById(id.edit_text).setEnabled(enable);
		findViewById(id.button_sign).setEnabled(enable);
		findViewById(id.button_auth).setEnabled(enable);
		findViewById(id.button_change_puk).setEnabled(enable);
		findViewById(id.button_change_pin1).setEnabled(enable);
		findViewById(id.button_change_pin2).setEnabled(enable);
		findViewById(id.button_unblock_pin1).setEnabled(enable);
		//findViewById(id.button_verify).setEnabled(enable);
	}

	public void connectReader(View view) {
		sminterface.connect(new Connected() {
			@Override
			public void connected() {
				enableButtons(sminterface != null);
			}
		});
	}

	public void changePin(View view) {
		EditText currentPinPuk = (EditText)findViewById(id.insert_pin);
		EditText newPinPuk = (EditText)findViewById(id.new_pin_puk);
		try {
			PinType type = null;
			switch (view.getId()) {
			case id.button_change_pin1: type = PinType.PIN1; break;
			case id.button_change_pin2: type = PinType.PIN2; break;
			case id.button_change_puk: type = PinType.PUK; break;
			}
			boolean status = EstEIDUtil.changePin(currentPinPuk.getText().toString().getBytes(),
					newPinPuk.getText().toString().getBytes(),
					type, sminterface);
			content.setText(status ? "PIN change success" : "PIN change failed");
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}

	public void unblockPin1(View view) {
		EditText currentPinPuk = (EditText)findViewById(id.insert_pin);
		try {
			boolean status = EstEIDUtil.unblockPin(currentPinPuk.getText().toString().getBytes(), PinType.PIN1, sminterface);
			content.setText(status ? "PIN unblock success" : "PIN unblock failed");
		} catch (Exception e) {
			e.printStackTrace();
			content.setText(e.getMessage());
		}
	}
}
