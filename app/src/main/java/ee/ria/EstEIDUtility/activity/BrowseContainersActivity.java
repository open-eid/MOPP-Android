package ee.ria.EstEIDUtility.activity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.R;
import ee.ria.libdigidocpp.digidoc;

public class BrowseContainersActivity extends AppCompatActivity {

    static {
        System.loadLibrary("digidoc_java");
    }

    private static final String TAG = "BrowseContainersActivity";

    public static final String BDOC_NAME = "ee.ria.EstEIDUtility.bdocName";
    public static final String FILE_NAME = "ee.ria.EstEIDUtility.fileName";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_containers);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initLibraryConfiguration();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void initLibraryConfiguration() {
        digidoc.initJava(getFilesDir().getAbsolutePath());
        try (ZipInputStream zis = new ZipInputStream(getResources().openRawResource(R.raw.schema))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                try (FileOutputStream out = openFileOutput(ze.getName(), Context.MODE_PRIVATE)) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "initLibraryConfiguration: ", e);
        }

        try (InputStream in = getResources().openRawResource(R.raw.test);
             FileOutputStream out = openFileOutput("testing.bdoc", Context.MODE_PRIVATE)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "createDummyFiles: ", e);
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("BrowseContainers Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
