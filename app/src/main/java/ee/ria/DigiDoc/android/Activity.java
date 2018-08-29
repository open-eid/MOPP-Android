package ee.ria.DigiDoc.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.auth.AuthenticationCreateScreen;
import ee.ria.DigiDoc.android.main.home.HomeScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;

import static ee.ria.DigiDoc.android.Constants.MESSAGING_HASH_KEY;
import static ee.ria.DigiDoc.android.Constants.MESSAGING_HASH_TYPE_KEY;
import static ee.ria.DigiDoc.android.Constants.MESSAGING_SESSION_ID_KEY;

public final class Activity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Navigator navigator;
    private RootScreenFactory rootScreenFactory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Application);
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() != null && getIntent().getExtras().getString(MESSAGING_HASH_KEY) != null) {
            openAuthScreen(savedInstanceState);
        }
        rootScreenFactory.intent(getIntent());
        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
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

        @Nullable
        private Intent intent;

        @Inject
        RootScreenFactory() {
        }

        void intent(Intent intent) {
            this.intent = intent;
        }

        @Override
        public Screen call() {
            return HomeScreen.create(intent);
        }
    }

    private void openAuthScreen(Bundle savedInstanceState) {
/////////////// How to get instance id /////////////////////////
//        FirebaseInstanceId.getInstance().getInstanceId()
//                .addOnCompleteListener(task -> {
//                    if (!task.isSuccessful()) {
//                        Log.w(TAG, "getInstanceId failed", task.getException());
//                        return;
//                    }
//                    // Get new Instance ID token
//                    String token = task.getResult().getToken();
//                    System.out.println("adsd");
//                });
        Bundle extras = getIntent().getExtras();

        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
        navigator.execute(Transaction.root(AuthenticationCreateScreen.create(extras.getString(MESSAGING_HASH_KEY), extras.getString(MESSAGING_HASH_TYPE_KEY), extras.getString(MESSAGING_SESSION_ID_KEY))));
    }
}
