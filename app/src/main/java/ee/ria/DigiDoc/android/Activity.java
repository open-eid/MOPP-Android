package ee.ria.DigiDoc.android;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.home.HomeScreen;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

public final class Activity extends AppCompatActivity {

    private Navigator navigator;
    private RootScreenFactory rootScreenFactory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Application);
        setTitle(""); // ACCESSIBILITY: prevents application name read during each activity launch
        super.onCreate(savedInstanceState);

        if (!BuildConfig.BUILD_TYPE.contentEquals("develop")) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            handleIncomingFiles(intent);
        } else {
            rootScreenFactory.intent(getIntent());
        }

        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
    }

    private void handleIncomingFiles(Intent intent) {
        try {
            intent.setDataAndType(intent.getData(), "*/*");
            rootScreenFactory.intent(intent);
        } catch (ActivityNotFoundException e) {
            e.getStackTrace();
        }

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Application.ApplicationComponent component = Application.component(newBase);
        navigator = component.navigator();
        rootScreenFactory = component.rootScreenFactory();
        super.attachBaseContext(component.localeService().attachBaseContext(newBase));
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

    @Singleton
    static final class RootScreenFactory implements Callable<Screen> {

        @Nullable private Intent intent;

        @Inject RootScreenFactory() {}

        void intent(Intent intent) {
            this.intent = intent;
        }


        @Override
        public Screen call() {
            if (intent.getAction() != null && Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
                return SignatureCreateScreen.create(intent);
            }
            return HomeScreen.create(intent);
        }
    }


}
