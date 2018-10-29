/*
 * Copyright 2017 Riigi Infosüsteemide Amet
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

package ee.ria.DigiDoc.mobileid.soap;

import android.content.res.Resources;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.VisitorStrategy;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import ee.ria.DigiDoc.mobileid.BuildConfig;
import ee.ria.DigiDoc.mobileid.R;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import timber.log.Timber;

public class ServiceGenerator {

    private static OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .certificatePinner(new CertificatePinner.Builder().add("digidocservice.sk.ee",
                    "sha256/69i1y4v6AUAp3dDArytNsYC0GjRqMDIPYRI78k/Ig6I=",
                    "sha256/21pSl8+gYIgzVrmihoLqfp4XlT3k97qo+M2ZGRQsy4Q=",
                    "sha256/VhdNOtlxqJRJZLGJgR8wCEk6apBCLjxYBOyDjU+U9iI=").build());
    private static HttpLoggingInterceptor loggingInterceptor;

    private static Retrofit retrofit;

    public static <S> S createService(Resources resources, Class<S> serviceClass, SSLContext sslContext) {
        if (retrofit == null) {
            Timber.d("Creating new retrofit instance");
            retrofit = new Retrofit.Builder()
                    .baseUrl(resources.getString(R.string.mobile_id_service_url))
                    .addConverterFactory(SimpleXmlConverterFactory.create(visitorStrategySerializer()))
                    .client(buildHttpClient(sslContext))
                    .build();
        }
        Timber.d("Creating service client instance");
        return retrofit.create(serviceClass);
    }

    static Retrofit retrofit() {
        return retrofit;
    }

    private static OkHttpClient buildHttpClient(SSLContext sslContext) {
        Timber.d("Building new httpClient");
        addLoggingInterceptor();
        if (sslContext != null) {
            try {
                httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory());
            } catch (Exception e) {
                Timber.e(e, "Error building httpClient with sslContext");
            }
        }
        return httpClientBuilder.build();
    }

    private static void addLoggingInterceptor() {
        if (BuildConfig.DEBUG) {
            Timber.d("adding logging interceptor to http client");
            if (loggingInterceptor == null) {
                loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            if (!httpClientBuilder.interceptors().contains(loggingInterceptor)) {
                httpClientBuilder.addInterceptor(loggingInterceptor);
            }
        }
    }

    private static Serializer visitorStrategySerializer() {
        return new Persister(new VisitorStrategy(new RequestObjectInterceptor()));
    }
}
