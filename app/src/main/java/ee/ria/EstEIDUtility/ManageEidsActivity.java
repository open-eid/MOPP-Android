package ee.ria.EstEIDUtility;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;

public class ManageEidsActivity extends AppCompatActivity {

    private static final String TAG = "ManageEidsActivity";

    TextView content;
    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;
    private boolean serviceBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        content = (TextView) findViewById(R.id.eids_content);
        enableButtons(false);

        ServiceCreatedCallback callback = new TokenServiceCreatedCallback();
        tokenServiceConnection = new TokenServiceConnection(this, callback);
        tokenServiceConnection.connectService();
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
            enableButtons(true);
            serviceBound = true;
            Toast.makeText(ManageEidsActivity.this, "Service connected", Toast.LENGTH_LONG).show();
        }

        @Override
        public void failed() {
            Log.e(TAG, "failed: ", null);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            serviceBound = false;
            enableButtons(false);
            Toast.makeText(ManageEidsActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
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

    public void displayPersonalData(View view) throws RemoteException {

        Log.d(TAG, "displayPersonalData: " + tokenService);

        PersonalFileCallback callback = new PersonalFileCallback() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                Log.d(TAG, "onPersonalFileResponse: " + result);
                content.setText("Cert common name: " + result);
            }

            @Override
            public void onPersonalFileError(String msg) {
                Log.d(TAG, "onPersonalFileError: " + msg);
            }
        };
        tokenService.readPersonalFile(callback);
    }

    public void displayCertInfo(View view) throws RemoteException {
        Log.d(TAG, "displayCertInfo: " + tokenService);

        CertificateInfoCallback callback = new CertificateInfoCallback();
        tokenService.readCertificateInHex(Token.CertType.CertSign, callback);
    }

    class CertificateInfoCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            String certificateInHex = Util.toHex(cert);
            content.setText("Cert common name: " + Util.getCommonName(Util.fromHex(certificateInHex)));
        }

        @Override
        public void onCertificateError(String reason) {
            new AlertDialog.Builder(ManageEidsActivity.this)
                    .setTitle(R.string.cert_read_failed)
                    .setMessage(reason)
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        }
    }

    private void enableButtons(boolean enable) {
        findViewById(R.id.read_cert).setEnabled(enable);
        findViewById(R.id.read_personal_data).setEnabled(enable);
    }

}
