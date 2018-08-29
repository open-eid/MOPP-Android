package ee.ria.DigiDoc.android;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Intent intent = new Intent(this, AuthenticationActivity.class);
            Map<String, String> message = remoteMessage.getData();
            for(Map.Entry<String, String> entry : message.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
            startActivity(intent);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // token = c8c0FgG7xOw:APA91bGDafsoV2QNUA9a_w7YhlhvQ9iYoiNt4oagvWNBPkeMNZ7ydv02PyvQTnQHviziEXX22wHPEDb_a_3olB5Qr8yeOe_IYcESXlrX5mYUaY181Yet6Ta_JTdT7H6lvZq91iuBFGJHxtmqWUKvK7hHZQGGP9IFGQ

    }

}