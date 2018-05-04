package ee.ria.DigiDoc.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.home.HomeScreen;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

public final class Activity extends AppCompatActivity {

    private Navigator navigator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Application);
        super.onCreate(savedInstanceState);
        Application.ApplicationComponent component = Application.component(this);
        component.rootScreenFactory().intent(getIntent());
        navigator = component.navigator();
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

    @Singleton
    static final class RootScreenFactory implements Callable<Screen> {

        @Nullable private Intent intent;

        @Inject RootScreenFactory() {}

        void intent(Intent intent) {
            this.intent = intent;
        }

        @Override
        public Screen call() {
            if (intent != null && TextUtils.equals(intent.getAction(), Intent.ACTION_VIEW)
                    && intent.getData() != null) {
                return SignatureCreateScreen.create(intent);
            }
            return HomeScreen.create();
        }
    }
}
