package ee.ria.EstEIDUtility;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

public class ManageEidsActivity extends AppCompatActivity {

    TextView content;
    private SMInterface sminterface = null;
    byte[] signCert;
    private EstEIDToken eidToken;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        content = (TextView) findViewById(R.id.eids_content);
        enableButtons(false);
    }

    @Override
    protected void onResume () {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (nfcAdapter == null) {
            return;
        }
        sminterface = new SMInterface.NFC((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                enableButtons(sminterface != null);
            }
        });
        eidToken = new EstEIDToken(sminterface, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sminterface != null) {
            sminterface.close();
        }
    }

    public void connectReader(View view) {
        sminterface = SMInterface.getInstance(this, SMInterface.ACS);
        if (sminterface == null) {
            content.setText("No readers connected");
            return;
        }
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                enableButtons(sminterface != null);
            }
        });
        eidToken = new EstEIDToken(sminterface, this);
    }

    public void displayPersonalData(View view){
        eidToken.setPersonalFileListener(new Token.PersonalFileListener() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                content.setText("Personal data:\n" + result);
            }
            @Override
            public void onPersonalFileError(String msg) {
                content.setText(msg);
            }
        });
        eidToken.readPersonalFile();
    }

    public void displayCertInfo(View view){
        if (eidToken != null) {
            eidToken.setCertListener(new Token.CertListener() {
                @Override
                public void onCertificateResponse(Token.CertType type, byte[] cert) {
                    content.setText("Cert common name: " + Util.getCommonName(signCert = cert));
                }

                @Override
                public void onCertificateError(String msg) {
                    new AlertDialog.Builder(ManageEidsActivity.this)
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
        }
    }

    public void enableButtons(boolean enable) {
        findViewById(R.id.read_cert).setEnabled(enable);
        findViewById(R.id.read_personal_data).setEnabled(enable);
    }
}
