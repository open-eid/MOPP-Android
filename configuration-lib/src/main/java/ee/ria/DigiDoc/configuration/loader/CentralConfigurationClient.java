package ee.ria.DigiDoc.configuration.loader;

import static ee.ria.DigiDoc.common.ProxyUtil.getManualProxySettings;
import static ee.ria.DigiDoc.common.ProxyUtil.getProxySetting;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.ProxyConfig;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.ProxyUtil;
import ee.ria.DigiDoc.configuration.R;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.tls.OkHostnameVerifier;
import timber.log.Timber;

class CentralConfigurationClient {

    private static final int DEFAULT_TIMEOUT = 5;
    private final Context context;
    private final OkHttpClient httpClient;
    private final String centralConfigurationServiceUrl;
    private final String userAgent;

    CentralConfigurationClient(Context context, String centralConfigurationServiceUrl, String userAgent) {
        this.context = context;
        this.centralConfigurationServiceUrl = centralConfigurationServiceUrl;
        httpClient = constructHttpClient(context);
        this.userAgent = userAgent;
    }

    String getConfiguration() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.json");
        future.exceptionally(e -> {
            Timber.log(Log.ERROR, e, String.format("%s %s", "Unable to get configuration", e.getLocalizedMessage()));
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show());
            return future.join();
        });
        return future.join();
    }

    String getConfigurationSignature() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.rsa");
        future.exceptionally(e -> {
            Timber.log(Log.ERROR, e, String.format("%s. %s", "Unable to get configuration signature", e.getLocalizedMessage()));
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show());
            return future.join();
        });
        return future.join();
    }

    String getConfigurationSignaturePublicKey() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.pub");
        future.exceptionally(e -> {
            Timber.log(Log.ERROR, e, String.format("%s %s", "Unable to get configuration public key", e.getLocalizedMessage()));
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show());
            return future.join();
        });
        return future.join();
    }

    private CompletableFuture<String> requestData(String url) {
        CompletableFuture<String> result = new CompletableFuture<>();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", userAgent)
                .build();

        if (httpClient != null) {
            CompletableFuture.runAsync(() -> {
                Call call = httpClient.newCall(request);
                try {
                    Response response = call.execute();
                    if (response.code() != 200) {
                        result.completeExceptionally(new CentralConfigurationException("Service responded with not OK status code " + response.code()));
                        return;
                    }
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        result.completeExceptionally(new CentralConfigurationException("Service responded with empty body"));
                        return;
                    }
                    result.complete(responseBody.string());
                } catch (IOException e) {
                    result.completeExceptionally(new CentralConfigurationException("Something went wrong during fetching configuration", e));
                }
            });
        } else {
            result.completeExceptionally(new CentralConfigurationException("Unable to fetch configuration"));
        }

        return result;
    }

    private static OkHttpClient constructHttpClient(Context context) {
        try {
            OkHttpClient.Builder builder = constructClientBuilder(context);
            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HTTP client", e);
        }
    }

    private static OkHttpClient.Builder constructClientBuilder(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .hostnameVerifier(OkHostnameVerifier.INSTANCE)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        if (context != null) {
            ProxySetting proxySetting = getProxySetting(context);
            ManualProxy manualProxy = getManualProxySettings(context);
            ProxyConfig proxyConfig = ProxyUtil.getProxy(proxySetting, manualProxy);

            builder.proxy(proxySetting == ProxySetting.NO_PROXY ? Proxy.NO_PROXY : proxyConfig.proxy())
                    .proxyAuthenticator(proxySetting == ProxySetting.NO_PROXY ? Authenticator.NONE : proxyConfig.authenticator());

            builder.addInterceptor(chain -> {
                Request originalRequest = chain.request();
                String credential = Credentials.basic(manualProxy.getUsername(), manualProxy.getPassword());
                Request.Builder requestBuilder = originalRequest.newBuilder()
                        .addHeader("Proxy-Authorization", credential)
                        .addHeader("Authorization", credential);

                Request newRequest = requestBuilder.build();
                return chain.proceed(newRequest);
            });
        }

        return builder;
    }

    static class CentralConfigurationException extends RuntimeException {
        CentralConfigurationException(String message) {
            super(message);
        }

        CentralConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}