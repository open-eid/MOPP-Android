package ee.ria.EstEIDUtility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.digidoc;

public class ManageContainerActivity extends AppCompatActivity
        implements ContainerInfoFragment.OnFragmentInteractionListener, FileItemFragment.OnListFragmentInteractionListener {

    private static final int CHOOSE_FILE_REQUEST = 1;

    private Signature signature;
    private Container container;
    private SMInterface sminterface = null;
    private EstEIDToken eidToken;
    private TextView debug;

    private void connectReader() {
        sminterface = SMInterface.getInstance(this, SMInterface.ACS);
        if (sminterface == null) {
            return;
        }
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                appendDebug("SMINTERFACE_CONNECTED", "true");
            }
        });
        eidToken = new EstEIDToken(sminterface, this);
    }

    private void prepareSignature(){
        if (eidToken != null) {
            eidToken.setCertListener(new Token.CertListener() {
                @Override
                public void onCertificateResponse(Token.CertType type, byte[] cert) {
                    appendDebug("GOT_CERT", "true");
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
            });
            eidToken.readCert(EstEIDToken.CertType.CertSign);
        }
    }

    private void signContainer(byte[] hashToSign) {
        appendDebug("HASH_TO_SIGN", String.valueOf(hashToSign.length));
        eidToken.setSignListener(new Token.SignListener() {
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
        });

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(new byte[] { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20 }); // SHA256 OID
            outputStream.write(hashToSign);
            eidToken.sign(Token.PinType.PIN2, outputStream.toByteArray());
        } catch(Exception e) {
            e.printStackTrace();
            appendDebug("SIGN_EXCEPTION", e.getMessage());
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
        connectReader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sminterface != null) {
            sminterface.close();
        }
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
