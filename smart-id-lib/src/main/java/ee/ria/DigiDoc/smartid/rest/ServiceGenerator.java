/*
 * smart-id-lib
 * Copyright 2020 Riigi Infosüsteemi Amet
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

import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.smartid.BuildConfig;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class ServiceGenerator {

    private static HttpLoggingInterceptor loggingInterceptor;

    public static <S> S createService(Class<S> serviceClass, String sidSignServiceUrl, ArrayList<String> certBundle)
            throws CertificateException, NoSuchAlgorithmException {
        Timber.d("Creating new retrofit instance");
        return new Retrofit.Builder()
                .baseUrl(sidSignServiceUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildHttpClient(sidSignServiceUrl, certBundle))
                .build()
                .create(serviceClass);
    }

    private static OkHttpClient buildHttpClient(String sidSignServiceUrl, ArrayList<String> certBundle)
            throws CertificateException, NoSuchAlgorithmException {
        Timber.d("Building new httpClient");
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .certificatePinner(trustedCertificates(sidSignServiceUrl, certBundle))
                .cache(null);
        addLoggingInterceptor(httpClientBuilder);
        return httpClientBuilder.build();
    }

    private static void addLoggingInterceptor(OkHttpClient.Builder httpClientBuilder) {
        if (BuildConfig.DEBUG) {
            Timber.d("Adding logging interceptor to HTTP client");
            if (loggingInterceptor == null) {
                loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            if (!httpClientBuilder.interceptors().contains(loggingInterceptor)) {
                httpClientBuilder.addInterceptor(loggingInterceptor);
            }
        }
    }

    private static CertificatePinner trustedCertificates(String sidSignServiceUrl, ArrayList<String> certBundle)
            throws CertificateException, NoSuchAlgorithmException {
        URI uri;
        try {
            uri = new URI(sidSignServiceUrl);
        } catch (URISyntaxException e) {
            Timber.e(e, "Failed to convert URI from URL");
            return new CertificatePinner.Builder().build();
        }
        String[] sha256Certificates = new String[certBundle.size()];
        try {
            for (int i = 0; i < certBundle.size(); i++) {
                sha256Certificates[i] = "sha256/" + getSHA256FromCertificate(CertificateUtil.x509Certificate(Base64.decode(certBundle.get(i))));
            }
        } catch (CertificateException | NoSuchAlgorithmException e) {
            Timber.e(e, "Failed to convert to Certificate object");
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
            Timber.e(e, "Unable to get instance of algorithm");
            throw e;
        }
    }
}
