package ee.ria.DigiDoc.configuration.loader;

import static ee.ria.DigiDoc.common.NetworkUtil.DEFAULT_TIMEOUT;
import static ee.ria.DigiDoc.common.NetworkUtil.constructClientBuilder;
import static ee.ria.DigiDoc.configuration.util.LocalizationUtil.getLocalizedMessage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import ee.ria.DigiDoc.configuration.R;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.tls.OkHostnameVerifier;
import timber.log.Timber;

class CentralConfigurationClient {

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
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, getLocalizedMessage(context, R.string.no_internet_connection), Toast.LENGTH_LONG).show());
            return future.join();
        });
        return future.join();
    }

    String getConfigurationSignature() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.rsa");
        future.exceptionally(e -> {
            Timber.log(Log.ERROR, e, String.format("%s. %s", "Unable to get configuration signature", e.getLocalizedMessage()));
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, getLocalizedMessage(context, R.string.no_internet_connection), Toast.LENGTH_LONG).show());
            return future.join();
        });
        return future.join();
    }

    String getConfigurationSignaturePublicKey() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.pub");
        future.exceptionally(e -> {
            Timber.log(Log.ERROR, e, String.format("%s %s", "Unable to get configuration public key", e.getLocalizedMessage()));
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, getLocalizedMessage(context, R.string.no_internet_connection), Toast.LENGTH_LONG).show());
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

    private OkHttpClient constructHttpClient(Context context) {
        try {
            if (context == null) {
                return new OkHttpClient.Builder()
                        .hostnameVerifier(OkHostnameVerifier.INSTANCE)
                        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .callTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS).build();
            }
            return constructClientBuilder(context).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HTTP client", e);
        }
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