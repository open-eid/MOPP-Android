package ee.ria.DigiDoc.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.security.SecureRandom;

import ee.ria.DigiDoc.android.auth.AuthenticationCreateScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

import static ee.ria.DigiDoc.android.Constants.MESSAGING_HASH_KEY;
import static ee.ria.DigiDoc.android.Constants.MESSAGING_HASH_TYPE_KEY;
import static ee.ria.DigiDoc.android.Constants.MESSAGING_SESSION_ID_KEY;


public class AuthenticationActivity extends AppCompatActivity {
    private Navigator navigator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
        Bundle extras = getIntent().getExtras();
        navigator.execute(Transaction.root(AuthenticationCreateScreen.create(extras.getString(MESSAGING_HASH_KEY), extras.getString(MESSAGING_HASH_TYPE_KEY), extras.getString(MESSAGING_SESSION_ID_KEY))));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void attachBaseContext(Context newBase) {

        Application.ApplicationComponent component = Application.component(newBase);
        navigator = component.navigator();
        super.attachBaseContext(component.localeService().attachBaseContext(newBase));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        navigator.onActivityResult(requestCode, resultCode, data);
    }

}
