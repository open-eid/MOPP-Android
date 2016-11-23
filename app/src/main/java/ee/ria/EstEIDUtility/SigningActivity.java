package ee.ria.EstEIDUtility;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.Signature;

import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;

public class SigningActivity extends AppCompatActivity {

    TextView content;
    private SMInterface sminterface = null;
    byte[] signCert, signedBytes;
    private EstEIDToken eidToken;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        content = (TextView) findViewById(R.id.sign_content);
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

    public void signText(final View view) {
        eidToken.setSignListener(new Token.SignListener() {
            @Override
            public void onSignResponse(byte[] signature) {
                content.setText(Util.toHex(signedBytes = signature));
                findViewById(R.id.button_verify).setEnabled(signCert != null && signature != null);
            }

            @Override
            public void onSignError(String msg) {
                content.setText(msg);
            }
        });

        if (view.getId() == R.id.button_sign) {
            try {
                EditText textToSign = (EditText)findViewById(R.id.textToSign);
                byte[] textDigest = MessageDigest.getInstance("SHA-1").digest(
                        textToSign.getText().toString().getBytes());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14}); // SHA1 OID
                outputStream.write(textDigest);
                eidToken.sign(Token.PinType.PIN2, outputStream.toByteArray());
            } catch(Exception e) {
                e.printStackTrace();
                content.setText(e.getMessage());
            }
        } else {
            eidToken.sign(Token.PinType.PIN1,
                    new byte[] {0x3F, 0x4B ,(byte) 0xE6 ,0x4B ,(byte) 0xC9 ,0x06 ,0x6F ,0x14 ,(byte) 0x8A ,0x39 ,0x21 ,(byte) 0xD8 ,0x7C ,(byte) 0x94 ,0x41 ,0x40 ,(byte) 0x99 ,0x72 ,0x4B ,0x58 ,0x75 ,(byte) 0xA1 ,0x15 ,0x78 });
        }
    }

    public void verify(View view){
        EditText textToSign = (EditText)findViewById(R.id.textToSign);
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(Util.getX509Certificate(signCert).getPublicKey());
            sig.update(textToSign.getText().toString().getBytes());
            content.setText("Signature verify: " + sig.verify(signedBytes));
        } catch (Exception e){
            e.printStackTrace();
            content.setText("Signature verify: failed\n" + e.getMessage());
        }
    }

    public void enableButtons(boolean enable) {
        findViewById(R.id.button_sign).setEnabled(enable);
        findViewById(R.id.button_auth).setEnabled(enable);
        findViewById(R.id.button_verify).setEnabled(enable);
    }

    public void createNewContainer(View view) {
        EditText containerName = (EditText) findViewById(R.id.textToSign);

        String fileName = containerName.getText().toString();
        if (fileName == null || fileName.isEmpty()) {
            NotificationUtil.showNotification(this, getResources().getString(R.string.file_name_empty_message), NotificationUtil.NotificationType.WARNING);
            return;
        }

        if (!FilenameUtils.getExtension(containerName.getText().toString()).equals(Constants.BDOC_EXTENSION)) {
            containerName.append(".");
            containerName.append(Constants.BDOC_EXTENSION);
            containerName.setText(containerName.getText().toString());
        }

        String bdocFileName = containerName.getText().toString();
        if (FileUtils.fileExists(getFilesDir().getAbsolutePath(), bdocFileName)) {
            NotificationUtil.showNotification(this, R.string.file_exists_message, NotificationUtil.NotificationType.WARNING);
            return;
        }

        Intent intent = new Intent(this, BdocDetailActivity.class);
        intent.putExtra(Constants.BDOC_NAME, bdocFileName);
        startActivity(intent);
    }

}
