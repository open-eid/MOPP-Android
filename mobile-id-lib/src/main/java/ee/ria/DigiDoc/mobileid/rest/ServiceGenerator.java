/*
 * Copyright 2017 - 2022 Riigi Infosüsteemi Amet
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

import android.util.Log;

import org.bouncycastle.util.encoders.Base64;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.mobileid.BuildConfig;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import timber.log.Timber;

public class ServiceGenerator {

    private static final String CERT_PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String CERT_PEM_FOOTER = "-----END CERTIFICATE-----";

    private static HttpLoggingInterceptor loggingInterceptor;

    public static <S> S createService(Class<S> serviceClass, SSLContext sslContext, String midSignServiceUrl, ArrayList<String> certBundle, TrustManager[] trustManagers) throws CertificateException, NoSuchAlgorithmException {
        Timber.log(Log.DEBUG, "Creating new retrofit instance");
        return new Retrofit.Builder()
                .baseUrl(midSignServiceUrl + "/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildHttpClient(sslContext, midSignServiceUrl, certBundle, trustManagers))
                .build()
                .create(serviceClass);
    }

    private static OkHttpClient buildHttpClient(SSLContext sslContext, String midSignServiceUrl, ArrayList<String> certBundle, TrustManager[] trustManagers) throws CertificateException, NoSuchAlgorithmException {
        Timber.log(Log.DEBUG, "Building new httpClient");
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .certificatePinner(trustedCertificates(midSignServiceUrl, certBundle));
        addLoggingInterceptor(httpClientBuilder);
        if (sslContext != null) {
            try {
                httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
            } catch (Exception e) {
                Timber.log(Log.ERROR, e, "Error building httpClient with sslContext");
            }
        }
        return httpClientBuilder.build();
    }

    private static void addLoggingInterceptor(OkHttpClient.Builder httpClientBuilder) {
        if (BuildConfig.DEBUG) {
            Timber.log(Log.DEBUG, "Adding logging interceptor to HTTP client");
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
                Timber.log(Log.ERROR, e, "Failed to convert to Certificate object");
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
            Timber.log(Log.ERROR, e, "Failed to convert URI from URL");
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
            Timber.log(Log.ERROR, e, "Unable to get instance of algorithm");
            throw e;
        }
    }
}
