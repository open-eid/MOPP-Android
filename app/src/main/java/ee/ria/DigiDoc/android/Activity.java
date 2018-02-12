package ee.ria.DigiDoc.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.home.HomeScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

public final class Activity extends AppCompatActivity {

    private Navigator navigator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Application);
        super.onCreate(savedInstanceState);
        navigator = Application.component(this).navigator();
        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (!navigator.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        navigator.onActivityResult(requestCode, resultCode, data);
    }

    static final class RootScreenFactory implements Callable<Screen> {

        @Inject RootScreenFactory() {}

        @Override
        public Screen call() throws Exception {
            return HomeScreen.create();
        }
    }
}
