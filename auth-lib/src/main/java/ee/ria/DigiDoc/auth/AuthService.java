package ee.ria.DigiDoc.auth;

import android.support.annotation.WorkerThread;
import android.util.Base64;
import okhttp3.*;
import timber.log.Timber;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Properties;


public class AuthService {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String AUTH_REPLY_URL = "authentication/reply/";
    private static final String REGISTER_URL = "register/";

    @WorkerThread
    public final void sendRegisterRequest(String deviceId, String nationalIdentityNumber,  InputStream rawKeyStore, InputStream rawProperties){
        Properties properties = getProperties(rawProperties);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, requestRegisterJson(deviceId, nationalIdentityNumber));
        OkHttpClient client = getSSLClient(rawKeyStore, properties);
        executeClientCall(client, body, properties.getProperty("authentication-url") + REGISTER_URL);
    }

    @WorkerThread
    public final void sendAuthResponse(ByteBuffer signature, String sessionId, String certificate, String hash,
                                       InputStream rawKeyStore, InputStream rawProperties) {

        Properties properties = getProperties(rawProperties);
        byte[] signatureBytes = new byte[signature.remaining()];
        signature.get(signatureBytes, 0, signatureBytes.length);

        String encodedSignature = new String(Base64.encode(signatureBytes, Base64.NO_WRAP));
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, requestAuthResponeJson(encodedSignature, sessionId, certificate, hash));

        OkHttpClient client = getSSLClient(rawKeyStore, properties);
        executeClientCall(client, body, properties.getProperty("authentication-url") + AUTH_REPLY_URL);
    }


    private void executeClientCall(OkHttpClient client, RequestBody body, String url) {
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Timber.d("Request sent successfully");
            } else {
                Timber.w("Could not send the request");
            }

        } catch (IOException e) {
            throw new AuthServerConnectionException();
        }
    }

    private Properties getProperties(InputStream rawProperties) {
        Properties properties = new Properties();
        try {
            properties.load(rawProperties);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Could not open properties file", e);
        }
    }

    private OkHttpClient getSSLClient(InputStream rawKeyStore, Properties properties) {
        try {
            String keyStoreType = "PKCS12";
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(rawKeyStore, properties.getProperty("keystore-password").toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, trustManager).build();


        } catch (Exception e) {
            throw new AuthServerConnectionException();
        }
    }

    private String requestRegisterJson(String deviceId, String nationalIdentityNumber) {
        return "{\"deviceId\":\"" + deviceId + "\","
                + "\"nationalIdentityNumber\":\"" + nationalIdentityNumber + "\""
                + "}";
    }

    private String requestAuthResponeJson(String signature, String sessionId, String certificate, String hash) {
        return "{\"result\":\"OK\","
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"signature\":\"" + signature + "\","
                + "\"hash\":\"" + hash + "\","
                + "\"cert\":\"" + certificate + "\""
                + "}";
    }
}
