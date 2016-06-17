package ee.ria.EstEIDUtility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.InputType;
import android.util.Xml;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MobileIDToken extends Token {
	private SSLContext sc;
	private Activity parent;

	public MobileIDToken(Activity parent) throws Exception {
		this.parent = parent;

		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(parent.getResources().openRawResource(R.raw.sk878252), "aPQ11ti4".toCharArray());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, "aPQ11ti4".toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init((KeyStore) null);

		//TODO trust SK root cert on older devices
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			}
		};

		sc = SSLContext.getInstance("TLS");
		sc.init(kmf.getKeyManagers(), /*tmf.getTrustManagers()*/ trustAllCerts, null);
	}

	private static class XMLResult {
	 	String fault = null, message = null, sessionCode = null, status = null, challengeId = null;
	 	byte[] cert  = null, signature = null;
	 	Exception e = null;
	}

	static XMLResult sendXML(String personalCode, String mobileNR, SSLContext sc, String request) throws IOException, XmlPullParserException {
		String t = personalCode + mobileNR;
		URL url = t.equals("14212128021" + "+37200003") ||
			t.equals("14212128022" + "+37200004") ||
			t.equals("14212128023" + "+37200005") ||
			t.equals("14212128024" + "+37200006") ||
			t.equals("14212128025" + "+37200007") ||
			t.equals("14212128026" + "+37200008") ||
			t.equals("14212128027" + "+37200009") ||
			t.equals("38002240211" + "+37200001") ||
			t.equals("14212128029" + "+37200001066") ?
			new URL("https://tsp.demo.sk.ee") : new URL("https://digidocservice.sk.ee");

		byte[] content = (
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"+
			"<SOAP-ENV:Envelope"+
			" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""+
			" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
			" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"+
			"   <SOAP-ENV:Body>"+
			request+
			"   </SOAP-ENV:Body>"+
			"</SOAP-ENV:Envelope>").getBytes();

		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setSSLSocketFactory(sc.getSocketFactory());
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setDoOutput(true);
		connection.setFixedLengthStreamingMode(content.length);
		OutputStream os = connection.getOutputStream();
		os.write(content);
		os.close();

		if (!connection.getContentType().contains("text/xml")) {
			throw new IOException("Invalid ContentType " + connection.getContentType());
		}

		InputStream is = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
		XmlPullParser xml = Xml.newPullParser();
		xml.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		xml.setInput(is, null);

		XMLResult r = new XMLResult();
		String name = "";
		while (xml.next() != XmlPullParser.END_DOCUMENT) {
			switch (xml.getEventType()) {
				case XmlPullParser.START_TAG: name = xml.getName(); continue;
				case XmlPullParser.TEXT: break;
				default: continue;
			}

			if (name.equals("faultstring")) {
				r.fault = xml.getText();
			} else if (name.equals("message")) {
				r.message = xml.getText();
			} else if (name.equals("AuthCertStatus")) {
				r.status = xml.getText();
			} else if (name.equals("AuthCertData")) {
				r.cert = xml.getText().getBytes();
			} else if (name.equals("ChallengeID")) {
				r.challengeId = xml.getText();
			} else if (name.equals("Sesscode")) {
				r.sessionCode = xml.getText();
			} else if (name.equals("Status")) {
				r.status = xml.getText();
			} else if (name.equals("Signature")) {
				r.signature = xml.getText().getBytes();
			}
		}
		is.close();
		if (r.fault != null) {
			throw new IOException(r.fault);
		}
		return r;
	}

	@Override
	public void sign(PinType type, byte[] data) {
		final byte[] localData = data;
		final AlertDialog challenge = new AlertDialog.Builder(parent)
			.setTitle("Sign with Mobile-ID")
			.setMessage("Signing in progress")
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			}).create();

		final AsyncTask<String, String, XMLResult> exec = new AsyncTask<String, String, XMLResult>() {
			@Override
			protected void onProgressUpdate(String... args) {
				challenge.setMessage(String.format("Signing in progress\nMake sure verification code matches with one in phone screen\n" +
						"Verification code: %s", args[0]));
			}

			@Override
			protected XMLResult doInBackground(String... args) {
				XMLResult r = new XMLResult();
				try {
					r = sendXML(args[0], args[1], sc, String.format(
						"       <d:MobileSignHashRequest"+
						"        	xmlns:d=\"http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl\">"+
						"           <IDCode xsi:type=\"xsd:string\">%s</IDCode>"+
						"           <PhoneNo xsi:type=\"xsd:string\">%s</PhoneNo>"+
						"           <Language xsi:type=\"xsd:string\">EST</Language>"+
						"           <MessageToDisplay xsi:type=\"xsd:string\">Signeerimine</MessageToDisplay>"+
						"           <ServiceName xsi:type=\"xsd:string\">DigiDoc3</ServiceName>"+
						"           <Hash xsi:type=\"xsd:string\">%s</Hash>"+
						"           <HashType xsi:type=\"xsd:enumeration\">SHA1</HashType>"+
						"           <KeyID xsi:type=\"xsd:string\">RSA</KeyID>"+
						"       </d:MobileSignHashRequest>",
						args[0],
						args[1],
						Util.toHex(localData)
						));

					if (r.status == null) {
						return r;
					}
					publishProgress(r.challengeId);

					String sessionCode = r.sessionCode;
					while (challenge.isShowing() && (r.status.equals("OK") || r.status.equals("OUTSTANDING_TRANSACTION"))) {
						Thread.sleep(5 * 1000);
						r = sendXML(args[0], args[1], sc, String.format(
							"       <d:GetMobileSignHashStatusRequest xmlns:d=\"http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl\">"+
							"           <Sesscode xsi:type=\"xsd:int\">%s</Sesscode>"+
							"           <WaitSignature xsi:type=\"xsd:boolean\">0</WaitSignature>"+
							"       </d:GetMobileSignHashStatusRequest>", sessionCode));
					}
					if (!challenge.isShowing()) {
						r.status = "USER_CANCEL";
					}
				} catch (Exception e) {
					r.e = e;
				}
				return r;
			}

			@Override
			protected void onPostExecute(XMLResult r) {
				challenge.cancel();
				if (r.e != null || r.fault != null) {
				// Skip status parsing
				} else if (r.status.equals("SIGNATURE")) {
					signListener.onSignResponse(r.signature);
					return;
				} else if (r.status.equals("USER_CANCEL")) {
					return;
				} else if (r.status.equals("EXPIRED_TRANSACTION")) {
					r.message = "Request timed out";
				} else if (r.status.equals("SENDING_ERROR")) {
					r.message = "Failed to send request";
				}
				if (r.message != null) {
					signListener.onSignError(r.message);
				}
			};
		};

		final EditText mobileNR = new EditText(parent);
		mobileNR.setInputType(InputType.TYPE_CLASS_PHONE);
		mobileNR.setHint("+372 123456");
		mobileNR.setText(parent.getSharedPreferences("Settings", 0).getString("mobileNR", ""));

		final EditText personalCode = new EditText(parent);
		personalCode.setInputType(InputType.TYPE_CLASS_NUMBER);
		personalCode.setHint("47101010027");
		personalCode.setText(parent.getSharedPreferences("Settings", 0).getString("personalCode", ""));

		LinearLayout layout = new LinearLayout(parent);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(mobileNR);
		layout.addView(personalCode);

		new AlertDialog.Builder(parent)
			.setTitle("Sign with Mobile-ID")
			.setView(layout)
			.setPositiveButton("Sign", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences.Editor editor = parent.getSharedPreferences("Settings", 0).edit();
					editor.putString("mobileNR", mobileNR.getText().toString());
					editor.putString("personalCode", personalCode.getText().toString());
					editor.commit();
					challenge.show();
					exec.execute(personalCode.getText().toString(), mobileNR.getText().toString());
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			}).show();
	}

	@Override
	public void readCert(CertType certType) {
		(new AsyncTask<String, Void, XMLResult>() {
			@Override
			protected XMLResult doInBackground(String... args) {
				XMLResult r = new XMLResult();
				try {
					r = sendXML(args[0], args[1], sc, String.format(
						"       <d:GetMobileCertificate"+
						"        xmlns:d=\"http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl\">"+
						"           <IDCode xsi:type=\"xsd:string\">%s</IDCode>"+
						"           <Country xsi:type=\"xsd:string\">EE</Country>"+
						"           <PhoneNo xsi:type=\"xsd:string\">%s</PhoneNo>"+
						"           <ReturnCertData xsi:type=\"xsd:string\">auth</ReturnCertData>"+
						"       </d:GetMobileCertificate>",
						args[0],
						args[1]
						));
				} catch (Exception e) {
					r.e = e;
				}
				return r;
			}

			@Override
			protected void onPostExecute(XMLResult r) {
				if (r.e != null) {
					certListener.onCertificateError(r.e.getMessage());
				} else {
					certListener.onCertificateResponse(CertType.CertAuth, r.cert);
				}
			}
		}).execute(
				parent.getSharedPreferences("Settings", 0).getString("personalCode", ""),
				parent.getSharedPreferences("Settings", 0).getString("mobileNR", ""));
	}
}
