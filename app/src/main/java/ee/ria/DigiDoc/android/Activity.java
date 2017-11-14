package ee.ria.DigiDoc.android;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.HomeView;

public final class Activity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Application);
        super.onCreate(savedInstanceState);
        HomeView homeView = new HomeView(this);
        homeView.setId(R.id.mainHome);
        setContentView(homeView);
    }
}
