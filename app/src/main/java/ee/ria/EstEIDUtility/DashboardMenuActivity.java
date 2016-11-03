package ee.ria.EstEIDUtility;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class DashboardMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private void startActivity(Class<?> signingActivityClass) {
        Intent intent = new Intent(this, signingActivityClass);
        startActivity(intent);
    }

}
