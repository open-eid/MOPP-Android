package ee.ria.DigiDoc.common;

import androidx.annotation.Nullable;

import java.net.Proxy;

import okhttp3.Authenticator;

public final class ProxyConfig {
    @Nullable
    private final Proxy proxy;
    private final Authenticator authenticator;
    @Nullable
    private final ManualProxy manualProxy;

    public ProxyConfig(@Nullable Proxy proxy, Authenticator authenticator, @Nullable ManualProxy manualProxy) {
        this.proxy = proxy;
        this.authenticator = authenticator;
        this.manualProxy = manualProxy;
    }

    @Nullable
    public Proxy proxy() {
        return proxy;
    }

    public Authenticator authenticator() {
        return authenticator;
    }

    @Nullable
    public ManualProxy manualProxy() {
        return manualProxy;
    }
}
