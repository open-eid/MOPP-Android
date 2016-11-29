package ee.ria.EstEIDUtility;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.Signature;

import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.Util;
import ee.ria.token.tokenservice.callback.SignCallback;

public class SigningActivity extends AppCompatActivity {

    private static final String TAG = "SigningActivity";
    TextView content;
    byte[] signCert, signedBytes;

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;

    @Override
    protected void onStart() {
        super.onStart();
        ServiceCreatedCallback callback = new TokenServiceCreatedCallback();
        tokenServiceConnection = new TokenServiceConnection(this, callback);
        tokenServiceConnection.connectService();
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
            enableButtons(true);
            Toast.makeText(SigningActivity.this, "Service connected", Toast.LENGTH_LONG).show();
        }

        @Override
        public void failed() {
            Log.e(TAG, "failed: ", null);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            enableButtons(false);
            Toast.makeText(SigningActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        content = (TextView) findViewById(R.id.sign_content);
        enableButtons(false);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tokenServiceConnection != null) {
            unbindService(tokenServiceConnection);
        }
    }

    public void signText(final View view) {
        EditText textToSign = (EditText)findViewById(R.id.textToSign);
        String pin = textToSign.getText().toString();

        SignCallback callback = new SignTaskCallback();

        if (view.getId() == R.id.button_sign) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] textDigest = MessageDigest.getInstance("SHA-1").digest(textToSign.getText().toString().getBytes());
                outputStream.write(new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14});
                outputStream.write(textDigest);

                tokenService.sign(Token.PinType.PIN2, pin, outputStream.toByteArray(), callback);
            } catch(Exception e) {
                Log.e(TAG, "signText: ", e);
                content.setText(e.getMessage());
            }
        } else {
            tokenService.sign(Token.PinType.PIN1, pin,
                    new byte[] {0x3F, 0x4B ,(byte) 0xE6 ,0x4B ,(byte) 0xC9 ,0x06 ,0x6F ,0x14 ,(byte) 0x8A ,0x39 ,0x21 ,(byte) 0xD8 ,0x7C ,(byte) 0x94 ,0x41 ,0x40 ,(byte) 0x99 ,0x72 ,0x4B ,0x58 ,0x75 ,(byte) 0xA1 ,0x15 ,0x78 },
                    callback);
        }
    }

    class SignTaskCallback implements SignCallback {

        @Override
        public void onSignResponse(byte[] signature) {
            content.setText(Util.toHex(signedBytes = signature));
            findViewById(R.id.button_verify).setEnabled(signCert != null && signature != null);
        }

        @Override
        public void onSignError(String msg) {
            content.setText(msg);
        }
    }

    public void verify(View view){
        String textToSign = "Some text please";
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(Util.getX509Certificate(signCert).getPublicKey());
            sig.update(textToSign.getBytes());
            content.setText("Signature verify: " + sig.verify(signedBytes));
        } catch (Exception e){
            Log.e(TAG, "verify: ", e);
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
        if (FileUtils.bdocExists(getFilesDir(), bdocFileName)) {
            NotificationUtil.showNotification(this, R.string.file_exists_message, NotificationUtil.NotificationType.WARNING);
            return;
        }

        Intent intent = new Intent(this, BdocDetailActivity.class);
        intent.putExtra(Constants.BDOC_NAME, bdocFileName);
        startActivity(intent);
    }

}
