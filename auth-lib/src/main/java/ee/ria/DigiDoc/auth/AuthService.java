package ee.ria.DigiDoc.auth;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;

import android.util.Base64;
import okhttp3.*;
import timber.log.Timber;

import java.io.IOException;
import java.nio.ByteBuffer;



public class AuthService {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String AUTHENTICATION_URL = "http://192.168.77.115:8081/authentication/reply/";

    @WorkerThread
    public final void sendAuthResponse(ByteBuffer signature, String sessionId, String certificate, String hash) {
        OkHttpClient client = new OkHttpClient();
        byte[] signatureBytes = new byte[signature.remaining()];
        signature.get(signatureBytes, 0, signatureBytes.length);

        String encodedSignature = new String(Base64.encode(signatureBytes,Base64.NO_WRAP));
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, requestJson(encodedSignature, sessionId, certificate, hash));
        Request request = new Request.Builder()
                .url(AUTHENTICATION_URL)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if(response.isSuccessful()) {
                Timber.d("Authentication response sent successfully");
            } else {
                Timber.w("Could not send the authentication response");
            }

        } catch (IOException e) {
            throw new AuthServerConnectionException();
        }
    }

    String requestJson(String signature, String sessionId, String certificate, String hash) {
        return "{\"result\":\"OK\","
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"signature\":{"
                    + "\"value\":\"" + signature + "\","
                    + "\"algorithm\":\"ECDSA\""
                + " },"
                + "\"hash\":\"" + hash + "\","
                + "\"cert\":\"" + certificate + "\""
                + "}";
    }
}
