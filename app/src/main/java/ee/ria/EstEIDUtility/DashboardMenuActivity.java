package ee.ria.EstEIDUtility;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.libdigidocpp.digidoc;

public class DashboardMenuActivity extends AppCompatActivity {

    private static final String TAG = "DashboardMenuActivity";

    static {
        System.loadLibrary("digidoc_java");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        digidoc.initJava(getFilesDir().getAbsolutePath());
        setContentView(R.layout.activity_dashboard_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initLibraryConfiguration();
    }

    private void initLibraryConfiguration() {
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
    }

    public void startSign(View view) {
        startActivity(SigningActivity.class);
    }

    public void startMyEids(View view) {
        startActivity(ManageEidsActivity.class);
    }

    public void startPinUtilities(View view) {
        startActivity(PinUtilitiesActivity.class);
    }

    public void startContainerBrowse(View view) {
        startActivity(BrowseContainersActivity.class);
    }

    private void startActivity(Class<?> signingActivityClass) {
        Intent intent = new Intent(this, signingActivityClass);
        startActivity(intent);
    }

}
