package ee.ria.EstEIDUtility.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.digidoc;

public class DashboardMenuActivity extends AppCompatActivity {

    private static final String TAG = "DashboardMenuActivity";

    static {
        System.loadLibrary("digidoc_java");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File schemaPath = FileUtils.getSchemaPath(getFilesDir());
        digidoc.initJava(schemaPath.getAbsolutePath());
        setContentView(R.layout.activity_dashboard_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initLibraryConfiguration();
    }

    private void initLibraryConfiguration() {
        //FileUtils.removeAllFiles(getFilesDir());
        //FileUtils.showAllFiles(getFilesDir());

        File bdocsFilesPath = FileUtils.getBdocsFilesPath(getFilesDir());
        if (!bdocsFilesPath.exists()) {
            boolean mkdirs = bdocsFilesPath.mkdirs();
            if (mkdirs) {
                Log.d(TAG, "initLibraryConfiguration: created bdocs and bdocs/files directories");
            }
        }

        File schemaPath = FileUtils.getSchemaPath(getFilesDir());
        try {
            ZipInputStream zis = new ZipInputStream(getResources().openRawResource(R.raw.schema));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File entryFile = new File(schemaPath, ze.getName());
                FileOutputStream out = new FileOutputStream(entryFile);
                IOUtils.copy(zis, out);
                out.close();
            }
            zis.close();
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
