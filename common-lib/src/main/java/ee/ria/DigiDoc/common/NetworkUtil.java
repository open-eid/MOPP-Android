package ee.ria.DigiDoc.common;

import static ee.ria.DigiDoc.common.ProxyUtil.getManualProxySettings;
import static ee.ria.DigiDoc.common.ProxyUtil.getProxySetting;

import android.content.Context;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.internal.tls.OkHostnameVerifier;

public class NetworkUtil {

    public static final int DEFAULT_TIMEOUT = 5;

    public static OkHttpClient.Builder constructClientBuilder(Context context) {
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
}
