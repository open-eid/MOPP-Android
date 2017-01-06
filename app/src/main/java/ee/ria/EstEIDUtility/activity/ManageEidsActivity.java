package ee.ria.EstEIDUtility.activity;

import android.app.Service;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

import java.security.cert.CertificateParsingException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.domain.X509Cert;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.DateUtils;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.UseCounterCallback;
import ee.ria.token.tokenservice.token.Token;

public class ManageEidsActivity extends AppCompatActivity {

    private static final String TAG = "ManageEidsActivity";

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;
    private boolean serviceBound;

    private TextView givenNames;
    private TextView surnameView;
    private TextView documentNumberView;
    private TextView cardValidity;
    private TextView cardValidityTime;
    private TextView certValidity;
    private TextView certValidityTime;
    private TextView certUsedView;
    private TextView personIdCode;
    private TextView dateOfBirth;
    private TextView nationalityView;
    private TextView emailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);

        TextView info = (TextView) findViewById(R.id.info);
        info.setText(Html.fromHtml(getString(R.string.eid_info)));
        info.setMovementMethod(LinkMovementMethod.getInstance());

        givenNames = (TextView) findViewById(R.id.givenNames);
        surnameView = (TextView) findViewById(R.id.surname);
        documentNumberView = (TextView) findViewById(R.id.document_number);
        cardValidity = (TextView) findViewById(R.id.card_validity);
        cardValidityTime = (TextView) findViewById(R.id.card_valid_value);

        certValidity = (TextView) findViewById(R.id.cert_validity);
        certValidityTime = (TextView) findViewById(R.id.cert_valid_value);
        certUsedView = (TextView) findViewById(R.id.cert_used);

        personIdCode = (TextView) findViewById(R.id.person_id);
        dateOfBirth = (TextView) findViewById(R.id.date_of_birth);
        nationalityView = (TextView) findViewById(R.id.nationality);
        emailView = (TextView) findViewById(R.id.email);

        ServiceCreatedCallback callback = new TokenServiceCreatedCallback();
        tokenServiceConnection = new TokenServiceConnection(this, callback);
        tokenServiceConnection.connectService();
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
            serviceBound = true;
            readPersonalData();
            readCertInfo();
        }

        @Override
        public void failed() {
            Log.e(TAG, "failed: ", null);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            serviceBound = false;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    public void readPersonalData() {
        PersonalFileCallback callback = new PersonalFileCallback() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                String surname = result.get(1);
                String firstName = result.get(2);
                String firstName2 = result.get(3);

                String expiryDate = result.get(9);
                String identificationCode = result.get(7);
                String birthDate = result.get(6);
                String nationality = result.get(5);

                String documentNumber = result.get(8);

                givenNames.setText(firstName);
                givenNames.append(firstName2);
                surnameView.setText(surname);

                documentNumberView.setText(documentNumber);

                certValidityTime.setText(expiryDate);
                cardValidityTime.setText(expiryDate);
                personIdCode.setText(identificationCode);
                dateOfBirth.setText(birthDate);
                nationalityView.setText(nationality);

                try {
                    Date expiry = DateUtils.DATE_FORMAT.parse(expiryDate);
                    if (!expiry.before(new Date()) && expiry.after(new Date())) {
                        cardValidity.setText(getText(R.string.eid_valid));
                        cardValidity.setTextColor(Color.GREEN);
                        cardValidityTime.setTextColor(Color.GREEN);
                    } else {
                        cardValidity.setText(getText(R.string.eid_invalid));
                        cardValidity.setTextColor(Color.RED);
                        cardValidityTime.setTextColor(Color.RED);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "onPersonalFileResponse: ", e);
                }

            }

            @Override
            public void onPersonalFileError(String msg) {
                Log.d(TAG, "onPersonalFileError: " + msg);
            }
        };
        tokenService.readPersonalFile(callback);
    }

    public void readCertInfo() {
        SignCertificateCallback callback = new SignCertificateCallback();
        tokenService.readCert(Token.CertType.CertSign, callback);

        AuthCertificateCallback authCertificateCallback = new AuthCertificateCallback();
        tokenService.readCert(Token.CertType.CertAuth, authCertificateCallback);
    }

    private class UseCounterTaskCallback implements UseCounterCallback {
        @Override
        public void onCounterRead(int counterByte) {
            certUsedView.setText(String.valueOf(counterByte));
        }

        @Override
        public void cardNotProvided() {
            Log.e(TAG, "cardNotProvided: ", new Exception("Why?"));
        }
    }

    class SignCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);

            tokenService.readUseCounter(Token.CertType.CertSign, new UseCounterTaskCallback());

            if (x509Cert.isValid()) {
                certValidity.setText(getText(R.string.eid_valid));
                certValidity.setTextColor(Color.GREEN);
                certValidityTime.setTextColor(Color.GREEN);
            } else {
                certValidity.setText(getText(R.string.eid_invalid));
                certValidity.setTextColor(Color.RED);
                certValidityTime.setTextColor(Color.RED);
            }
        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(ManageEidsActivity.this, getText(R.string.cert_read_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    class AuthCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);
            try {
                Collection<List<?>> subjectAlternativeNames = x509Cert.getCertificate().getSubjectAlternativeNames();
                if (subjectAlternativeNames == null) {
                    Log.d(TAG, "Couldn't read email");
                    return;
                }
                for (List subjectAlternativeName : subjectAlternativeNames) {
                    if ((Integer) subjectAlternativeName.get(0) == 1) {
                        emailView.setText((CharSequence) subjectAlternativeName.get(1));
                    }
                }
            } catch (CertificateParsingException e) {
                Log.e(TAG, "onCertificateResponse: ", e);
            }

        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(ManageEidsActivity.this, getText(R.string.cert_read_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

}
