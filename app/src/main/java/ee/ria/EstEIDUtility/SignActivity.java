package ee.ria.EstEIDUtility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import ee.ria.EstEIDUtility.SMInterface.Connected;
import ee.ria.EstEIDUtility.Token.PinType;
import ee.ria.EstEIDUtility.Token.SignListener;

public class SignActivity extends Activity {
	TextView content;
	byte[] dataToSign, signedHash;
	EstEIDToken eidToken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acitivity_sign);
		TextView t = (TextView)findViewById(R.id.note);
		t.setText("Digidoc klient tahab allkirjastada, oled kindel?");
		content = (TextView) findViewById(R.id.content);

		Bundle extras = getIntent().getExtras();
		byte[] byteArray = extras.getByteArray("hash");
		if (byteArray != null)
			content.setText("Hash to sign:\n" + Util.toHex(byteArray));

		// TODO: identify Diget Algorithm
		/*
		if (s.signatureMethod().equals("http://www.w3.org/2000/09/xmldsig#rsa-sha1")) {
		     oid = new byte[] { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14 };
		    } else if(s.signatureMethod().equals("http://www.w3.org/2001/04/xmldsig-more#rsa-sha224")) {
		     oid = new byte[] { 0x30, 0x2d, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x04, 0x05, 0x00, 0x04, 0x1c };
		    } else if(s.signatureMethod().equals("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")) {
		     oid = new byte[] { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20 };
		    } else if(s.signatureMethod().equals("http://www.w3.org/2001/04/xmldsig-more#rsa-sha384")) {
		     oid = new byte[] { 0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x02, 0x05, 0x00, 0x04, 0x30 };
		    } else if(s.signatureMethod().equals("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512")) {
		     oid = new byte[] { 0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x05, 0x00, 0x04, 0x40 };
		    }*/
		//outputStream.write(new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14}); // SHA1 OID
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(new byte[] { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20 }); // SHA256 OID
			outputStream.write(byteArray);
			dataToSign = outputStream.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SMInterface sminterface = SMInterface.getInstance(this, SMInterface.ACS);

		if (sminterface == null) {
			content.setText("No readers connected");
		};
		sminterface.connect(new Connected() {
			@Override
			public void connected() {
			}
		});

		eidToken = new EstEIDToken(sminterface, this);
	}

	public void signHash(View view) {
		eidToken.setSignListener(new SignListener() {
			@Override
			public void onSignResponse(byte[] signature) {
				signedHash = signature;
				content.setText("\nSigned hash:\n" +Util.toHex(signedHash));
			}
			@Override
			public void onSignError(String msg) {
				content.setText(msg);
			}
		});
		eidToken.sign(PinType.PIN2, dataToSign);
	}

	public void back(View view) {
		Intent result = new Intent();
		if (signedHash != null) {
			result.putExtra("signedHash", signedHash);
			setResult(Activity.RESULT_OK, result);
		}
		finish();
	}
}
