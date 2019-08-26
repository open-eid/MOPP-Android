package ee.ria.DigiDoc.configuration.loader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class CentralConfigurationClient {

    private static final OkHttpClient HTTP_CLIENT = constructHttpClient();
    private static final int DEFAULT_TIMEOUT = 10;
    private final String centralConfigurationServiceUrl;

    CentralConfigurationClient(String centralConfigurationServiceUrl) {
        this.centralConfigurationServiceUrl = centralConfigurationServiceUrl;
    }

    String getConfiguration() {
        return requestData(centralConfigurationServiceUrl + "/config.json");
    }

    String getConfigurationSignature() {
        return requestData(centralConfigurationServiceUrl + "/config.rsa");
    }

    String getConfigurationSignaturePublicKey() {
        return requestData(centralConfigurationServiceUrl + "/config.pub");
    }

    private String requestData(String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .build();

        Call call = HTTP_CLIENT.newCall(request);
        try {
            Response response = call.execute();
            if (response.code() != 200) {
                throw new CentralConfigurationException("Service responded with not OK status code " + response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new CentralConfigurationException("Service responded with empty body");
            }
            return responseBody.string();
        } catch (IOException e) {
            throw new CentralConfigurationException("Something went wrong during fetching configuration", e);
        }
    }

    private static OkHttpClient constructHttpClient() {
        try {
            return new OkHttpClient.Builder()
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .callTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    class CentralConfigurationException extends RuntimeException {
        CentralConfigurationException(String message) {
            super(message);
        }

        CentralConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}