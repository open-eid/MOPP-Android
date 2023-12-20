package ee.ria.DigiDoc.common;

import static java.net.Proxy.Type.HTTP;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.math.NumberUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

import okhttp3.Authenticator;
import okhttp3.Credentials;

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
                        String credential = Credentials.basic(systemProxy.getUsername(), systemProxy.getPassword());
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    };
                }
                return getProxyConfig(systemProxy, authenticator).join();
            }
            case MANUAL_PROXY -> {
                Authenticator authenticator = (route, response) -> {
                    String credential = Credentials.basic(manualProxySettings.getUsername(),
                            manualProxySettings.getPassword());
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };
                return getProxyConfig(manualProxySettings, authenticator).join();
            }
        }
        return new ProxyConfig(null, Authenticator.NONE);
    }

    public static boolean useHTTPSProxy(boolean isProxySSLEnabled, ManualProxy manualProxySettings) {
        String host = manualProxySettings.getHost();
        if (host != null) {
            return isProxySSLEnabled || !host.isEmpty() && !host.startsWith("https");
        }
        return false;
    }

    private static CompletableFuture<ProxyConfig> getProxyConfig(@Nullable ManualProxy manualProxy, Authenticator authenticator) {
        return CompletableFuture.supplyAsync(() -> {
            if (manualProxy != null && manualProxy.getHost() != null && !manualProxy.getHost().isEmpty()) {
                Proxy proxy = new Proxy(HTTP, new InetSocketAddress(manualProxy.getHost(), manualProxy.getPort()));
                return new ProxyConfig(proxy, authenticator != null ? authenticator : Authenticator.NONE);
            }
            return new ProxyConfig(null, authenticator != null ? authenticator : Authenticator.NONE);
        });
    }
}
