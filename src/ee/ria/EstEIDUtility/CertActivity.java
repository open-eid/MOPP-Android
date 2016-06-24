package ee.ria.EstEIDUtility;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import ee.ria.EstEIDUtility.SMInterface.Connected;
import ee.ria.EstEIDUtility.Token.CertListener;
import ee.ria.EstEIDUtility.Token.CertType;

public class CertActivity extends Activity {
	TextView content;
	byte[] certAuth, certSign;
	EstEIDToken eidToken;
	Intent result;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cert);
		content = (TextView) findViewById(R.id.content);
		result = new Intent();

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

	public void getCert(View view) {
		eidToken.setCertListener(new CertListener() {
			@Override
			public void onCertificateResponse(CertType type, byte[] cert) {
				switch (type) {
				case CertAuth:
					certAuth = cert;
					content.setText("\nCert:\n" +Util.toHex(cert));
					break;
				case CertSign:
					certSign = cert;
					content.setText("\nCert:\n" +Util.toHex(cert));
					break;
			}
			}
			@Override
			public void onCertificateError(String msg) {
				// TODO Auto-generated method stub
			}
		});
		eidToken.readCert(Token.CertType.CertSign);
	}

	public void back(View view) {
		Intent result = new Intent();
		if (certSign != null) {
			result.putExtra("cert", certSign);
			setResult(Activity.RESULT_OK, result);
		}
		finish();
	}
}
