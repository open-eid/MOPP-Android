package ee.ria.EstEIDUtility;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import ee.ria.aidl.token.tokenaidllibrary.CertificateResultListener;
import ee.ria.aidl.token.tokenaidllibrary.PersonalFileResultListener;
import ee.ria.aidl.token.tokenaidllibrary.TokenAidlInterface;

public class ManageEidsActivity extends AppCompatActivity {

    TextView content;
    private TokenAidlInterface service;
    private ManageEidsActivity.RemoteServiceConnection serviceConnection;
    private boolean serviceBound = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        content = (TextView) findViewById(R.id.eids_content);
        handler = new Handler(Looper.getMainLooper());
        enableButtons(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    private void connectService() {
        serviceConnection = new ManageEidsActivity.RemoteServiceConnection();
        Intent i = new Intent("ee.ria.aidl.token.tokenaidlservice.TokenService");
        i.setPackage("ee.ria.aidl.token.tokenaidlservice");
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void displayPersonalData(View view) throws RemoteException {
        service.readPersonalFile(new PersonalFileResultListener.Stub() {
            @Override
            public void onPersonalFileResponse(String personalFile) throws RemoteException {
                showResult("Personal data:\n" + personalFile);
            }

            @Override
            public void onPersonalFileError(String reason) throws RemoteException {
                showResult(reason);
            }
        });
    }

    public void displayCertInfo(View view) throws RemoteException {


        service.readSignCertificateInHex(new CertificateResultListener.Stub() {
            @Override
            public void onCertifiacteRequestSuccess(String certificateInHex) throws RemoteException {
                showResult("Cert common name: " + Util.getCommonName(Util.fromHex(certificateInHex)));
            }

            @Override
            public void onCertifiacteRequestFailed(String reason) throws RemoteException {
                showErrorDialog(reason);
            }
        });
    }

    private void showResult(final String textToShow) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                content.setText(textToShow);
            }
        });
    }

    private void showErrorDialog(final String reason) {
        handler.post(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public void enableButtons(boolean enable) {
        findViewById(R.id.read_cert).setEnabled(enable);
        findViewById(R.id.read_personal_data).setEnabled(enable);
    }

    class RemoteServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            service = TokenAidlInterface.Stub.asInterface((IBinder) boundService);
            serviceBound = true;
            enableButtons(true);
            Toast.makeText(ManageEidsActivity.this, "Service connected", Toast.LENGTH_LONG)
                    .show();
        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
            serviceBound = false;
            enableButtons(false);
            Toast.makeText(ManageEidsActivity.this, "Service disconnected", Toast.LENGTH_LONG)
                    .show();
        }

    }
}
