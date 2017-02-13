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

package ee.ria.mopp.androidmobileid.soap;

import android.util.Log;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.VisitorStrategy;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class ServiceGenerator {

    private static final String BASE_URL = "https://digidocservice.sk.ee/";

    private static Retrofit retrofit;

    private static Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create(visitorStrategySerializer()));

    private static OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

    private static HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY);

    public static <S> S createService(Class<S> serviceClass, SSLContext sslContext) {
        if (!httpClientBuilder.interceptors().contains(loggingInterceptor)) {
            httpClientBuilder.addInterceptor(loggingInterceptor);
            builder.client(buildHttpClient(sslContext));
            retrofit = builder.build();
        }
        return retrofit.create(serviceClass);
    }

    static Retrofit retrofit() {
        return retrofit;
    }

    private static OkHttpClient buildHttpClient(SSLContext sslContext) {
        if (sslContext != null) {
            try {
                return httpClientBuilder.connectTimeout(30, TimeUnit.SECONDS)
                        .sslSocketFactory(sslContext.getSocketFactory())
                        .build();
            } catch (Exception e) {
                Log.e("ServiceGenerator", "Exception", e);
            }
        }
        return httpClientBuilder.connectTimeout(30, TimeUnit.SECONDS).build();
    }

    private static Serializer visitorStrategySerializer() {
        return new Persister(new VisitorStrategy(new RequestObjectInterceptor()));
    }
}
