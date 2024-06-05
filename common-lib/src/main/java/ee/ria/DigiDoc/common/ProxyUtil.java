package ee.ria.DigiDoc.common;

import static java.net.Proxy.Type.HTTP;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Response;

public class ProxyUtil {

    public static ProxyConfig getProxy(ProxySetting proxySetting, ManualProxy manualProxySettings) {
        switch (proxySetting) {
            case NO_PROXY -> {}
            case SYSTEM_PROXY -> {
                ManualProxy systemProxy = new ManualProxy(
                        System.getProperty("http.proxyHost"),
                        NumberUtils.toInt(System.getProperty("http.proxyPort"), 0),
                        System.getProperty("http.proxyUser"),
                        System.getProperty("http.proxyPassword")
                );
                Authenticator authenticator = null;
                if (systemProxy.getUsername() != null && !systemProxy.getUsername().isEmpty() &&
                        systemProxy.getPassword() != null && !systemProxy.getPassword().isEmpty()) {
                    authenticator = (route, response) -> {
                        if (hasRetried(response)) {
                            return null;
                        }

                        String credential = Credentials.basic(systemProxy.getUsername(), systemProxy.getPassword());
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .header("Authorization", credential)
                                .build();
                    };
                }
                return getProxyConfig(systemProxy, authenticator).join();
            }
            case MANUAL_PROXY -> {
                Authenticator authenticator = (route, response) -> {
                    if (hasRetried(response)) {
                        return null;
                    }

                    String credential = Credentials.basic(manualProxySettings.getUsername(),
                            manualProxySettings.getPassword());
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .header("Authorization", credential)
                            .build();
                };
                return getProxyConfig(manualProxySettings, authenticator).join();
            }
        }
        return new ProxyConfig(null, Authenticator.NONE, null);
    }

    private static CompletableFuture<ProxyConfig> getProxyConfig(@Nullable ManualProxy manualProxy, Authenticator authenticator) {
        return CompletableFuture.supplyAsync(() -> {
            if (manualProxy != null && manualProxy.getHost() != null && !manualProxy.getHost().isEmpty()) {
                Proxy proxy = new Proxy(HTTP, new InetSocketAddress(manualProxy.getHost(), manualProxy.getPort()));
                return new ProxyConfig(proxy, authenticator != null ? authenticator : Authenticator.NONE, manualProxy);
            }
            return new ProxyConfig(null, authenticator != null ? authenticator : Authenticator.NONE, null);
        });
    }

    public static ProxySetting getProxySetting(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String proxySettingPreference = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_setting_key), ProxySetting.NO_PROXY.name());
        return ProxySetting.valueOf(proxySettingPreference);
    }

    public static ManualProxy getManualProxySettings(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String host = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_host_key), "");
        int port = sharedPreferences.getInt(context.getString(R.string.main_settings_proxy_port_key), 0);
        String username = sharedPreferences.getString(context.getString(R.string.main_settings_proxy_username_key), "");
        String password;
        try {
            password = EncryptedPreferences.getEncryptedPreferences(context).getString(context.getString(R.string.main_settings_proxy_password_key), "");
        } catch (IllegalStateException e) {
            password = "";
        }
        return new ManualProxy(host, port, username, password);
    }

    private static boolean hasRetried(Response response) {
        return !response.isSuccessful() &&
                response.request().header("Proxy-Authorization") != null;
    }
}
