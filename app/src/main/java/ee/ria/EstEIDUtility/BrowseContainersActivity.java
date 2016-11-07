package ee.ria.EstEIDUtility;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.digidoc;

public class BrowseContainersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_containers);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        createDummyFiles();
    }

    private void createDummyFiles() {
        String[] filenames = {"first.bdoc",
                "second.bdoc", "third.bdoc", "fourth.bdoc",
                "fifth.bdoc", "sixth.bdoc", "seventh.bdoc",
                "eight.bdoc", "ninth.bdoc", "tenth.bdoc",
                "eleventh.bdoc", "twelveth.bdoc"};

        for (String filename : filenames) {
            try (FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE)) {
                outputStream.write("Hello world!".getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
