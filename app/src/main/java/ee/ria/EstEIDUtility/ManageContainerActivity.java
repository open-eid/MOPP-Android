package ee.ria.EstEIDUtility;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.style.BCStyle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.domain.X509Cert;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.digidoc;
import ee.ria.token.tokenservice.*;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.SignCallback;

public class ManageContainerActivity extends AppCompatActivity
        implements ContainerInfoFragment.OnFragmentInteractionListener, FileItemFragment.OnListFragmentInteractionListener {

    private static final String TAG = "ManageContainerActivity";
    private static final int CHOOSE_FILE_REQUEST = 1;

    private Signature signature;
    private Container container;
    private TextView debug;

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
            Toast.makeText(ManageContainerActivity.this, "Service connected", Toast.LENGTH_LONG).show();
        }

        @Override
        public void failed() {
            Log.e(TAG, "failed: ", null);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            Toast.makeText(ManageContainerActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
        }
    }
    /*private void connectService() {
        ServiceConnection tokenServiceConnection = new TokenServiceConnection();
        Intent intent = new Intent(this, TokenService.class);
        bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);
    }

    class TokenServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
            tokenService = binder.getService();
            Toast.makeText(ManageContainerActivity.this, "Service connected", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            tokenService = null;
            Toast.makeText(ManageContainerActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
        }
    }*/

    private void prepareSignature() {
        CertCallback certCallback = new CertReadCallback(Token.CertType.CertSign);
        tokenService.readCertificateInHex(Token.CertType.CertSign, certCallback);
    }

    class CertReadCallback implements CertCallback {

        Token.CertType type;

        public CertReadCallback(Token.CertType type) {
            this.type = type;
        }

        @Override
        public void onCertificateResponse(byte[] cert) {
            appendDebug("GOT_CERT", "true");
            Log.d(TAG, "onCertificateResponse: " + type);
            signature = container.prepareWebSignature(cert);
            signContainer(signature.dataToSign());
        }

        @Override
        public void onCertificateError(String msg) {
            new AlertDialog.Builder(ManageContainerActivity.this)
                    .setTitle(R.string.cert_read_failed)
                    .setMessage(msg)
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        }
    }

    private void signContainer(byte[] hashToSign) {
        appendDebug("HASH_TO_SIGN", String.valueOf(hashToSign.length));
        final EditText pin = new EditText(this);
        pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);


        SignCallback callback = new SignTaskCallback();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(new byte[] { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20 }); // SHA256 OID
            outputStream.write(hashToSign);

            tokenService.sign(Token.PinType.PIN2, pin.getText().toString(), outputStream.toByteArray(), callback);
        } catch(Exception e) {
            Log.e(TAG, "signContainer: ", e);
            appendDebug("SIGN_EXCEPTION", e.getMessage());
        }
    }

    class SignTaskCallback implements SignCallback {

        @Override
        public void onSignResponse(byte[] signature) {
            appendDebug("SIGN_RESULT", "true");
            addSignatureToContainer(signature);
        }

        @Override
        public void onSignError(String msg) {
            appendDebug("SIGN_ERROR", msg);
            File dir = new File(getCacheDir().getPath());
            if (dir.exists()) {
                for (File f : dir.listFiles()) {
                    if (f.getName().equalsIgnoreCase("digidocpp.log")) {
                        String log = readAllText(f);
                        appendDebug("NATIVE_LOG", log);
                    }
                }
            }
        }
    }

    private void addSignatureToContainer(byte[] signatureValue) {
        try {
            signature.setSignatureValue(signatureValue);
            X509Cert x509Cert = new X509Cert(signature.signingCertificateDer());
            String surname = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.SURNAME));
            String name = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.GIVENNAME));
            String serialNumber = x509Cert.getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.SERIALNUMBER));
            appendDebug("SIGNED_BY: ", name + " " + surname + " " + serialNumber);
        } catch (Exception e) {
            appendDebug("ERROR_ADDING_SIGNATURE_TO_CONTAINER", e.getMessage());
            File dir = new File(getCacheDir().getPath());
            if (dir.exists()) {
                for (File f : dir.listFiles()) {
                    if (f.getName().equalsIgnoreCase("digidocpp.log")) {
                        String log = readAllText(f);
                        appendDebug("NATIVE_LOG", log);
                    }
                }
            }
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        debug = (TextView) findViewById(R.id.container_debug);
        loadLibDigidocpp();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tokenServiceConnection != null) {
            unbindService(tokenServiceConnection);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void appendDebug(String event, String text) {
        debug.append(event  + " : " + text);
        debug.append(System.lineSeparator());
        Log.i(event, text);
    }

    private void loadLibDigidocpp() {
        try {
            ZipInputStream zis = new ZipInputStream(getResources().openRawResource(R.raw.schema));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                FileOutputStream out = new FileOutputStream(new File(getCacheDir(), ze.getName()));
                IOUtils.copy(zis, out);
                out.close();
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.loadLibrary("digidoc_java");
        digidoc.initJava(getCacheDir().getAbsolutePath());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            addToFileList(uri);
        }
    }

    public void addFile(View view) {
        launchFileChooser();
    }

    public void signContainer(View view) {
        String path = getCacheDir().getAbsolutePath();
        appendDebug("SAVE_CONTAINER_PATH", getCacheDir().getPath() + " : " + path);
        container = Container.create(path + "/some_bdoc.bdoc");

        FileItemFragment fileItemFrag = (FileItemFragment) getSupportFragmentManager().findFragmentById(R.id.container_file_list_fragment);
        for (FileItem fileItem : fileItemFrag.getAddedFiles()) {
            container.addDataFile(fileItem.getLocation(), "application/pdf");
        }
        prepareSignature();
    }

    public void saveContainer(View view) {
        ContainerInfoFragment containerInfo = (ContainerInfoFragment) getSupportFragmentManager().findFragmentById(R.id.container_info_fragment);
        String containerName = containerInfo.getContainerName();
        container.save(getFilesDir().getAbsolutePath() + "/" + containerName + ".bdoc");
    }

    public static String readAllText(File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            return readAllText(inputStream);
        } catch(Exception ex) {
            return null;
        }
    }

    public static String readAllText(InputStream inputStream) {
        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);

        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return text.toString();
    }

    private void addToFileList(Uri uri) {
        FileItem fileItem = resolveFileItemFromUri(uri);
        FileItemFragment fileItemFrag = (FileItemFragment) getSupportFragmentManager().findFragmentById(R.id.container_file_list_fragment);
        if (fileItem != null && fileItemFrag != null) {
            fileItemFrag.addFile(fileItem);
        }
    }

    private FileItem resolveFileItemFromUri(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            String fileName = resolveFileName(uri);
            File file = new File(getCacheDir(), fileName);
            OutputStream output = new FileOutputStream(file);
            IOUtils.copy(input, output);
            appendDebug("TEMP_FILE_PATH_AND_NAME", fileName + " : " + file.getPath() + " : " + file.getAbsolutePath());
            return new FileItem(fileName, file.getPath(), 1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String resolveFileName(Uri uri) {
        String uriString = uri.toString();
        if (uriString.startsWith("content://")) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        } else if (uriString.startsWith("file://")) {
            return new File(uriString).getName();
        }
        return null;
    }

    private void launchFileChooser() {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(intent.createChooser(intent, "Select File to Add"), CHOOSE_FILE_REQUEST);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onListFragmentInteraction(FileItem item) {

    }
}
