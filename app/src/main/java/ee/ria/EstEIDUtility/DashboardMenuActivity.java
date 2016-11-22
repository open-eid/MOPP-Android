package ee.ria.EstEIDUtility;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.libdigidocpp.digidoc;

public class DashboardMenuActivity extends AppCompatActivity {

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
