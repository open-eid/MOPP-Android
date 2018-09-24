package ee.ria.DigiDoc.auth;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.util.Base64;
import okhttp3.*;
import timber.log.Timber;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Properties;


public class AuthService {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @WorkerThread
    public final void sendAuthResponse(ByteBuffer signature, String sessionId, String certificate, String hash,
                                       InputStream keystore, InputStream rawProperties) {
        Properties properties = new Properties();

        try {
            properties.load(rawProperties);
        } catch (IOException e) {
            throw new IllegalStateException("Could not open properties file", e);
        }

        OkHttpClient client = null;
        try {
                String keyStoreType = "PKCS12";
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(keystore, properties.getProperty("keystore-password").toCharArray());
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
                HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                }; //TODO: delete this in production
                client = new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, trustManager).hostnameVerifier(hostnameVerifier).build();


        } catch (Exception e) {
            throw new AuthServerConnectionException();
        }

        byte[] signatureBytes = new byte[signature.remaining()];
        signature.get(signatureBytes, 0, signatureBytes.length);

        String encodedSignature = new String(Base64.encode(signatureBytes, Base64.NO_WRAP));
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, requestJson(encodedSignature, sessionId, certificate, hash));
        Request request = new Request.Builder()
                .url(properties.getProperty("authentication-url"))
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
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
                + "\"signature\":\"" + signature + "\","
                + "\"hash\":\"" + hash + "\","
                + "\"cert\":\"" + certificate + "\""
                + "}";
    }
}
