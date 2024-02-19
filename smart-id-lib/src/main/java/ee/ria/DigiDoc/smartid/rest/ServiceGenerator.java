/*
 * smart-id-lib
 * Copyright 2017 - 2023 Riigi Infosüsteemi Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.smartid.rest;

import static ee.ria.DigiDoc.common.LoggingUtil.isLoggingEnabled;

import android.content.Context;
import android.util.Log;

import org.bouncycastle.util.encoders.Base64;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.ProxyConfig;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.ProxyUtil;
import ee.ria.DigiDoc.smartid.BuildConfig;
import okhttp3.Authenticator;
import okhttp3.CertificatePinner;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import timber.log.Timber;

public class ServiceGenerator {

    private static HttpLoggingInterceptor loggingInterceptor;

    public static <S> S createService(Class<S> serviceClass, String sidSignServiceUrl,
                                      ArrayList<String> certBundle,
                                      ProxySetting proxySetting, ManualProxy manualProxySettings,
                                      Context context)
            throws CertificateException, NoSuchAlgorithmException {
        Timber.log(Log.DEBUG, "Creating new retrofit instance");
        return new Retrofit.Builder()
                .baseUrl(sidSignServiceUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildHttpClient(sidSignServiceUrl, certBundle,
                        proxySetting, manualProxySettings, context))
                .build()
                .create(serviceClass);
    }

    private static OkHttpClient buildHttpClient(String sidSignServiceUrl,
                                                ArrayList<String> certBundle,
                                                ProxySetting proxySetting,
                                                ManualProxy manualProxySettings,
                                                Context context)
            throws CertificateException, NoSuchAlgorithmException {
        Timber.log(Log.DEBUG, "Building new httpClient");

        ProxyConfig proxyConfig = ProxyUtil.getProxy(proxySetting, manualProxySettings);

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .proxy(proxySetting == ProxySetting.NO_PROXY ? Proxy.NO_PROXY : proxyConfig.proxy())
                .proxyAuthenticator(proxySetting == ProxySetting.NO_PROXY ? Authenticator.NONE : proxyConfig.authenticator())
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .pingInterval(3, TimeUnit.SECONDS)
                .certificatePinner(trustedCertificates(sidSignServiceUrl, certBundle))
                .cache(null);
        addLoggingInterceptor(httpClientBuilder, context);
        return httpClientBuilder.build();
    }

    private static void addLoggingInterceptor(OkHttpClient.Builder httpClientBuilder, Context context) {
        if (isLoggingEnabled(context) || BuildConfig.DEBUG) {
            Timber.log(Log.DEBUG, "Adding logging interceptor to HTTP client");
            if (loggingInterceptor == null) {
                loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);
            }
            if (!httpClientBuilder.interceptors().contains(loggingInterceptor)) {
                httpClientBuilder.addInterceptor(loggingInterceptor);
            }
            httpClientBuilder.addInterceptor(chain -> {
                Request request = chain.request();
                if (isLoggingEnabled(context) || BuildConfig.DEBUG) {
                    Timber.log(Log.DEBUG, request.method() + " " + request.url());
                    Headers headers = request.headers();
                    Timber.log(Log.DEBUG, "Headers: " + Arrays.deepToString(new Headers[]{headers}));
                    RequestBody requestBody = request.body();
                    if (requestBody != null) {
                        try (Buffer buffer = new Buffer()) {
                            requestBody.writeTo(buffer);
                            Timber.log(Log.DEBUG, " Body: " + buffer.readUtf8());
                        }
                    }
                }
                return chain.proceed(request);
            });
        }
    }

    private static CertificatePinner trustedCertificates(String sidSignServiceUrl, ArrayList<String> certBundle)
            throws CertificateException, NoSuchAlgorithmException {
        URI uri;
        try {
            uri = new URI(sidSignServiceUrl);
        } catch (URISyntaxException e) {
            Timber.log(Log.ERROR, e, "Failed to convert URI from URL");
            return new CertificatePinner.Builder().build();
        }
        String[] sha256Certificates = new String[certBundle.size()];
        try {
            for (int i = 0; i < certBundle.size(); i++) {
                sha256Certificates[i] = "sha256/" + getSHA256FromCertificate(CertificateUtil.x509Certificate(Base64.decode(certBundle.get(i))));
            }
        } catch (CertificateException | NoSuchAlgorithmException e) {
            Timber.log(Log.ERROR, e, "Failed to convert to Certificate object");
            throw e;
        }
        return new CertificatePinner.Builder()
                .add(uri.getHost(), sha256Certificates)
                .build();
    }

    private static String getSHA256FromCertificate(Certificate cert) throws NoSuchAlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(cert.getPublicKey().getEncoded());
            byte[] base64EncodedHash = Base64.encode(encodedHash);
            return new String(base64EncodedHash, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            Timber.log(Log.ERROR, e, "Unable to get instance of algorithm");
            throw e;
        }
    }
}
