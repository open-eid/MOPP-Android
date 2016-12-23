package ee.ria.EstEIDUtility.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import ee.ria.EstEIDUtility.configuration.Configuration;

public abstract class EntryPointActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.init(getApplicationContext());
    }
}
