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


public class AuthenticationActivity extends AppCompatActivity {
    private Navigator navigator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
//        try {
//            MessageDigest digestMethod = MessageDigest.getInstance("SHA512");
//            byte[] response = digestMethod.digest(getRandomBytes());
//           String test = new String(Base64.encode(response));
        navigator.execute(Transaction.root(AuthenticationCreateScreen.create("7x/Oo6GisMauOdwmyGTgG3+pErQ+OOrLeVEN3yWXEGq/lUbJNq/1ffvexAYul5VcenOlU6wzUoUuFDzbugC5tA==")));
//        }catch(Exception e){
//                throw new RuntimeException("qwqwe");
//            }

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

    private static byte[] getRandomBytes() {
        byte randBytes[] = new byte[64];
        new SecureRandom().nextBytes(randBytes);
        return randBytes;
    }


}
