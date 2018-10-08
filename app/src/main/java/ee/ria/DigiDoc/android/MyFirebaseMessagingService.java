package ee.ria.DigiDoc.android;

import android.content.Intent;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import ee.ria.DigiDoc.R;

import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.auth.AuthService;
import okhttp3.*;
import timber.log.Timber;

import javax.inject.Inject;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Intent intent = new Intent(this, AuthenticationActivity.class);
            Map<String, String> message = remoteMessage.getData();
            for (Map.Entry<String, String> entry : message.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
            startActivity(intent);
        }
    }

    @Override
    public void onNewToken(String token) {
        AuthService authService = Application.AuthLibModule.authService();

        Log.d(TAG, "Refreshed token: " + token);
        InputStream rawKeyStore = getApplicationContext().getResources().openRawResource(ee.ria.DigiDoc.auth.R.raw.clientkeystore);
        InputStream rawProperties = getApplicationContext().getResources().openRawResource(R.raw.config);
         //TODO: change National identity number
        authService.sendRegisterRequest(token, "11111111111", rawKeyStore, rawProperties);
    }


}