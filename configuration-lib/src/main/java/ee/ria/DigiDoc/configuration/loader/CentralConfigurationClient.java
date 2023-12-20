package ee.ria.DigiDoc.configuration.loader;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.tls.OkHostnameVerifier;

class CentralConfigurationClient {

    private static final int DEFAULT_TIMEOUT = 5;
    private final OkHttpClient httpClient;
    private final String centralConfigurationServiceUrl;
    private final String userAgent;

    CentralConfigurationClient(Context context, String centralConfigurationServiceUrl, String userAgent) {
        this.centralConfigurationServiceUrl = centralConfigurationServiceUrl;
        httpClient = constructHttpClient(context);
        this.userAgent = userAgent;
    }

    String getConfiguration() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.json");
        return future.join();
    }

    String getConfigurationSignature() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.rsa");
        return future.join();
    }

    String getConfigurationSignaturePublicKey() {
        CompletableFuture<String> future = requestData(centralConfigurationServiceUrl + "/config.pub");
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

    private static ProxySetting getProxySetting(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String proxySettingPreference = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_setting_key), ProxySetting.NO_PROXY.name());
        return ProxySetting.valueOf(proxySettingPreference);
    }

    private static ManualProxy getManualProxySettings(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String host = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_host_key), "");
        int port = sharedPreferences.getInt(context.getString(R.string.main_settings_proxy_port_key), 0);
        String username = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_username_key), "");
        String password = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_password_key), "");
        return new ManualProxy(host, port, username, password);
    }

    private static boolean isProxySSLEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(R.string.main_settings_proxy_ssl_enabled_key), true);
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
            boolean isProxySSLEnabled = isProxySSLEnabled(context);
            ProxySetting proxySetting = getProxySetting(context);
            ProxyConfig proxyConfig = ProxyUtil.getProxy(proxySetting, getManualProxySettings(context));
            boolean useHTTPSProxy = proxySetting != ProxySetting.MANUAL_PROXY || ProxyUtil.useHTTPSProxy(isProxySSLEnabled, getManualProxySettings(context));

            builder.proxy(proxySetting == ProxySetting.NO_PROXY || !useHTTPSProxy ? Proxy.NO_PROXY : proxyConfig.proxy())
                    .proxyAuthenticator(proxySetting == ProxySetting.NO_PROXY || !useHTTPSProxy ? Authenticator.NONE : proxyConfig.authenticator());
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