/*
 * Copyright 2021 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.mobileid.rest;

import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.mobileid.BuildConfig;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class ServiceGenerator {

    private static final String CERT_PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String CERT_PEM_FOOTER = "-----END CERTIFICATE-----";

    private static HttpLoggingInterceptor loggingInterceptor;

    public static <S> S createService(Class<S> serviceClass, SSLContext sslContext, String midSignServiceUrl, ArrayList<String> certBundle) throws CertificateException, NoSuchAlgorithmException {
        Timber.d("Creating new retrofit instance");
        return new Retrofit.Builder()
                .baseUrl(midSignServiceUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildHttpClient(sslContext, midSignServiceUrl, certBundle))
                .build()
                .create(serviceClass);
    }

    private static OkHttpClient buildHttpClient(SSLContext sslContext, String midSignServiceUrl, ArrayList<String> certBundle) throws CertificateException, NoSuchAlgorithmException {
        Timber.d("Building new httpClient");
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .certificatePinner(trustedCertificates(midSignServiceUrl, certBundle));
        addLoggingInterceptor(httpClientBuilder);
        if (sslContext != null) {
            try {
                httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), getTrustManager());
            } catch (Exception e) {
                Timber.e(e, "Error building httpClient with sslContext");
            }
        }
        return httpClientBuilder.build();
    }

    private static X509TrustManager getTrustManager() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                checkTrustManagerCertificates(trustManagers, CERT_CHECK.CHECK_CLIENT, x509Certificates, s);

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                checkTrustManagerCertificates(trustManagers, CERT_CHECK.CHECK_SERVER, x509Certificates, s);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return getCertificateAcceptedIssuers(trustManagers);
            }
        };
    }

    private static X509Certificate[] getCertificateAcceptedIssuers(TrustManager[] trustManagers) {
        for (TrustManager trustManager: trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return ((X509TrustManager) trustManager).getAcceptedIssuers();
            }
        }

        return null;
    }

    private static void checkTrustManagerCertificates(TrustManager[] trustManagers, CERT_CHECK certCheck, X509Certificate[] x509Certificates, String s) throws CertificateException {
        for (TrustManager trustManager: trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                switch (certCheck) {
                    case CHECK_CLIENT:
                        ((X509TrustManager) trustManager).checkClientTrusted(x509Certificates, s);
                        break;
                    case CHECK_SERVER:
                        ((X509TrustManager) trustManager).checkServerTrusted(x509Certificates, s);
                        break;
                }
            }
        }
    }

    private static void addLoggingInterceptor(OkHttpClient.Builder httpClientBuilder) {
        if (BuildConfig.DEBUG) {
            Timber.d("Adding logging interceptor to HTTP client");
            if (loggingInterceptor == null) {
                loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);
            }
            if (!httpClientBuilder.interceptors().contains(loggingInterceptor)) {
                httpClientBuilder.addInterceptor(loggingInterceptor);
            }
        }
    }

    private static CertificatePinner trustedCertificates(String midSignServiceUrl, ArrayList<String> certBundle) throws CertificateException, NoSuchAlgorithmException {
        URI uri = toURI(midSignServiceUrl);

        if (uri != null) {
            String[] sha256Certificates = new String[certBundle.size()];
            try {
                for (int i = 0; i < certBundle.size(); i++) {
                    String pemCert = CERT_PEM_HEADER + "\n" + certBundle.get(i) + "\n" + CERT_PEM_FOOTER;
                    sha256Certificates[i] = "sha256/" + getSHA256FromCertificate(CertificateUtil.x509Certificate(pemCert));
                }
            } catch (CertificateException | NoSuchAlgorithmException e) {
                Timber.e(e, "Failed to convert to Certificate object");
                throw e;
            }

            CertificatePinner.Builder certificatePinner = new CertificatePinner.Builder()
                    .add(uri.getHost(), sha256Certificates);

            return certificatePinner.build();
        }

        return new CertificatePinner.Builder().build();
    }

    private enum CERT_CHECK {
        CHECK_CLIENT,
        CHECK_SERVER
    }

    private static URI toURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            Timber.e(e, "Failed to convert URI from URL");
            return null;
        }
    }

    private static String getSHA256FromCertificate(Certificate cert) throws NoSuchAlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(cert.getPublicKey().getEncoded());
            byte[] base64EncodedHash = Base64.encode(encodedHash);

            return new String(base64EncodedHash, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Unable to get instance of algorithm");
            throw e;
        }
    }
}
