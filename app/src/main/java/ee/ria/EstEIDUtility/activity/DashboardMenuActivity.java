package ee.ria.EstEIDUtility.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.preferences.SettingsActivity;

public class DashboardMenuActivity extends EntryPointActivity {

    private static final String TAG = "DashboardMenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
