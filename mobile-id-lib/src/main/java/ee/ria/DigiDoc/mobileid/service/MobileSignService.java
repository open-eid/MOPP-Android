/*
 * Copyright 2017 - 2024 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.mobileid.service;

import static ee.ria.DigiDoc.common.ProxySetting.MANUAL_PROXY;
import static ee.ria.DigiDoc.common.ProxySetting.NO_PROXY;
import static ee.ria.DigiDoc.common.SigningUtil.checkSigningCancelled;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CONFIG_URL;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_HOST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_PASSWORD;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_PORT;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_USERNAME;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.PROXY_SETTING;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SIGNING_ROLE_DATA;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.bouncycastle.util.encoders.Base64;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;

import ee.ria.DigiDoc.common.ContainerWrapper;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.MessageUtil;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.ProxyUtil;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.common.TrustManagerUtil;
import ee.ria.DigiDoc.common.UUIDUtil;
import ee.ria.DigiDoc.common.VerificationCodeUtil;
import ee.ria.DigiDoc.common.exception.SigningCancelledException;
import ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType;
import ee.ria.DigiDoc.mobileid.dto.request.GetMobileCreateSignatureSessionStatusRequest;
import ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.DigiDoc.mobileid.dto.request.PostMobileCreateSignatureCertificateRequest;
import ee.ria.DigiDoc.mobileid.dto.request.PostMobileCreateSignatureSessionRequest;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureCertificateResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileIdServiceResponse;
import ee.ria.DigiDoc.mobileid.dto.response.RESTServiceFault;
import ee.ria.DigiDoc.mobileid.rest.MIDRestServiceClient;
import ee.ria.DigiDoc.mobileid.rest.ServiceGenerator;
import ee.ria.libdigidocpp.Container;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class MobileSignService extends Worker {

    private static final String PEM_BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END_CERT = "-----END CERTIFICATE-----";

    public static final String TAG = MobileSignService.class.getName();

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 1000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 5 * 1000;
    private static final long TIMEOUT_CANCEL = 120 * 1000;
    private static boolean isCancelled = false;
    private final String roleData;
    private long timeout;

    private ContainerWrapper containerWrapper;

    private MIDRestServiceClient midRestServiceClient;

    private final String signatureRequest;
    private final String accessTokenPath;
    private final String accessTokenPass;
    private final ArrayList<String> certificateCertBundle;
    private final String configUrl;
    private final ProxySetting proxySetting;
    private final ManualProxy manualProxySettings;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public MobileSignService(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
        Timber.tag(TAG);

        isCancelled = false;

        roleData = workerParameters.getInputData().getString(SIGNING_ROLE_DATA);

        signatureRequest = workerParameters.getInputData().getString(CREATE_SIGNATURE_REQUEST);
        accessTokenPath = workerParameters.getInputData().getString(ACCESS_TOKEN_PATH);
        accessTokenPass = workerParameters.getInputData().getString(ACCESS_TOKEN_PASS);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String certBundleList = sharedPreferences.getString(CERTIFICATE_CERT_BUNDLE, "");

        Type arraylistType = new TypeToken<ArrayList<String>>() {}.getType();
        certificateCertBundle = new Gson().fromJson(certBundleList, arraylistType);

        configUrl = workerParameters.getInputData().getString(CONFIG_URL);

        proxySetting = ProxySetting.valueOf(workerParameters.getInputData().getString(PROXY_SETTING));

        if (MANUAL_PROXY.equals(proxySetting)) {
            String proxySettingString = workerParameters.getInputData().getString(MANUAL_PROXY_HOST);
            if (proxySettingString != null && proxySettingString.isEmpty()) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.GENERAL_ERROR));
                throw new IllegalArgumentException("Proxy setting cannot be empty");
            }
        }

        manualProxySettings = new ManualProxy(
                workerParameters.getInputData().getString(MANUAL_PROXY_HOST),
                workerParameters.getInputData().getInt(MANUAL_PROXY_PORT, 0),
                workerParameters.getInputData().getString(MANUAL_PROXY_USERNAME),
                workerParameters.getInputData().getString(MANUAL_PROXY_PASSWORD)
        );
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.log(Log.DEBUG, "Handling mobile sign worker");

        TrustManager[] trustManagers;
        try {
            trustManagers = TrustManagerUtil.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            Timber.log(Log.ERROR, e, "Unable to get Trust Managers. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            return Result.failure();
        }

        timeout = 0;
        MobileCreateSignatureRequest request = getRequestFromData(signatureRequest);
        RoleData roleDataRequest = getRoleDataFromData(roleData);
        if (request != null) {
            PostMobileCreateSignatureCertificateRequest certificateRequest = getCertificateRequest(request);
            Timber.log(Log.DEBUG, "Certificate request: %s", certificateRequest.toString());
            SSLContext restSSLConfig;
            try {
                Timber.log(Log.DEBUG, "Creating SSL config");
                restSSLConfig = createSSLConfig(accessTokenPath, accessTokenPass, trustManagers);
            } catch (Exception e) {
                Timber.log(Log.ERROR, e, "Can't create SSL config. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                restSSLConfig = null;
            }

            try {
                if (certificateCertBundle != null) {
                    midRestServiceClient = ServiceGenerator.createService(MIDRestServiceClient.class,
                            restSSLConfig, request.getUrl(), certificateCertBundle, trustManagers,
                            proxySetting, manualProxySettings, getApplicationContext());
                } else {
                    Timber.log(Log.DEBUG, "Certificate cert bundle is null");
                    return Result.failure();
                }
            } catch (CertificateException | NoSuchAlgorithmException e) {
                Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Invalid SSL handshake. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                return Result.failure();
            }

            if (isCountryCodeError(request.getPhoneNumber())) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_COUNTRY_CODE));
                Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. Invalid country code");
                return Result.failure();
            }

            if (!UUIDUtil.isValid(request.getRelyingPartyUUID())) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
                Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. %s - Relying Party UUID not in valid format", request.getRelyingPartyUUID());
                return Result.failure();
            }

            try {
                checkSigningCancelled(isCancelled);

                Call<MobileCreateSignatureCertificateResponse> call = midRestServiceClient.getCertificate(certificateRequest);

                Response<MobileCreateSignatureCertificateResponse> responseWrapper = call.execute();
                if (!responseWrapper.isSuccessful()) {
                    Timber.log(Log.DEBUG, "Mobile-ID certificate request unsuccessful. Status: %s, message: %s, body: %s, errorBody: %s",
                            responseWrapper.code(), responseWrapper.message(), responseWrapper.body(), responseWrapper.errorBody());
                    parseErrorAndBroadcast(responseWrapper);
                    return Result.failure();
                } else {
                    MobileCreateSignatureCertificateResponse response = responseWrapper.body();
                    if (response == null) {
                        Timber.log(Log.DEBUG, "Mobile-ID signature certificate response is null");
                        return Result.failure();
                    }
                    Timber.log(Log.DEBUG, "MobileCreateSignatureCertificateResponse response body: %s", response.toString());
                    if (isResponseError(responseWrapper, response, MobileCreateSignatureCertificateResponse.class)) {
                        return Result.failure();
                    }
                    containerWrapper = new ContainerWrapper(request.getContainerPath());
                    String base64Hash = containerWrapper.prepareSignature(getCertificatePem(response.getCert()), roleDataRequest);
                    if (base64Hash != null && !base64Hash.isEmpty()) {
                        Timber.log(Log.DEBUG, "Broadcasting create signature response");
                        broadcastMobileCreateSignatureResponse(base64Hash);
                        sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
                        String sessionId = getMobileIdSession(base64Hash, request);
                        if (sessionId == null) {
                            Timber.log(Log.DEBUG, "Session ID missing");
                            return Result.failure();
                        }
                        Timber.log(Log.DEBUG, "Session ID: %s", sessionId);
                        doCreateSignatureStatusRequestLoop(new GetMobileCreateSignatureSessionStatusRequest(sessionId));
                        countDownLatch.await();
                    } else {
                        Timber.log(Log.DEBUG, "Base64 (Prepare signature) is empty or null");
                        return Result.failure();
                    }
                }
            } catch (UnknownHostException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
                Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. REST API certificate request failed. Unknown host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                return Result.failure();
            } catch (SSLPeerUnverifiedException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. SSL handshake failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                return Result.failure();
            } catch (SocketTimeoutException ste) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
                Timber.log(Log.ERROR, ste, "Failed to sign with Mobile-ID. Unable to connect to service. Exception message: %s. Exception: %s", ste.getMessage(), Arrays.toString(ste.getStackTrace()));
                return Result.failure();
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("CONNECT: 403")) {
                    broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. REST API certificate request failed. Received HTTP status 403. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                } else if (e.getMessage() != null && (ProxyUtil.getProxySetting(getApplicationContext()) != NO_PROXY &&
                        e.getMessage().contains("Failed to authenticate with proxy"))) {
                    broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_PROXY_SETTINGS));
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. REST API certificate request failed with current proxy settings. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                }
                broadcastFault(defaultError(e.getMessage()));
                Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. REST API certificate request failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                return Result.failure();
            } catch (CertificateException e) {
                broadcastFault(defaultError(e.getMessage()));
                Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Generating certificate failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                return Result.failure();
            } catch (SigningCancelledException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.USER_CANCELLED));
                Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. User cancelled signing. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                return Result.failure();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS);
                    broadcastFault(fault);
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID - Too Many Requests. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                } else if (e.getMessage() != null && e.getMessage().contains("OCSP response not in valid time slot")) {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.OCSP_INVALID_TIME_SLOT);
                    broadcastFault(fault);
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID - OCSP response not in valid time slot. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                } else if (e.getMessage() != null && e.getMessage().contains("Certificate status: revoked")) {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.CERTIFICATE_REVOKED);
                    broadcastFault(fault);
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID - Certificate status: revoked. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                } else if (e.getMessage() != null && e.getMessage().contains("Failed to connect")) {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE);
                    broadcastFault(fault);
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID - Failed to connect to host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                } else if (e.getMessage() != null && e.getMessage().startsWith("Failed to create ssl connection with host")) {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE);
                    broadcastFault(fault);
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID - Failed to create ssl connection with host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                } else {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TECHNICAL_ERROR);
                    broadcastFault(fault);
                    Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Technical or general error. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                }
                return Result.failure();
            }
        }
        return Result.success();
    }

    public static void setIsCancelled(boolean isCancelled) {
        MobileSignService.isCancelled = isCancelled;
    }

    private String getCertificatePem(String cert) {
        return PEM_BEGIN_CERT + "\n" + cert + "\n" + PEM_END_CERT;
    }

    private static SSLContext createSSLConfig(String accessTokenPath, String accessTokenPass, TrustManager[] trustManagers) throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        try (InputStream key = new FileInputStream(accessTokenPath)) {
            String keyStoreType = "PKCS12";
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(key, accessTokenPass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, null);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.createSSLEngine().setEnabledProtocols(new String [] { "TLSv1.2", "TLSv1.3" });
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
            return sslContext;
        }
    }

    private void doCreateSignatureStatusRequestLoop(GetMobileCreateSignatureSessionStatusRequest request) throws IOException, SigningCancelledException {

            checkSigningCancelled(isCancelled);

            Call<MobileCreateSignatureSessionStatusResponse> responseCall = midRestServiceClient.getMobileCreateSignatureSessionStatus(request.getSessionId(), request.getTimeoutMs());
            Response<MobileCreateSignatureSessionStatusResponse> responseWrapper = responseCall.execute();
            Timber.log(Log.DEBUG, "MobileCreateSignatureSessionStatusResponse response: %s", responseWrapper.toString());
            if (!responseWrapper.isSuccessful()) {
                Timber.log(Log.DEBUG, "MobileCreateSignatureSessionStatusResponse responseWrapper unsuccessful: %s", responseWrapper.toString());
                parseErrorAndBroadcast(responseWrapper);
                return;
            }

            MobileCreateSignatureSessionStatusResponse response = responseWrapper.body();
            Timber.log(Log.DEBUG, "MobileCreateSignatureSessionStatusResponse response body: %s", response != null ? response.toString() : "null");
            if (response != null && isSessionStatusRequestComplete(response.getState())) {
                if (isResponseError(responseWrapper, response, MobileCreateSignatureSessionStatusResponse.class)) {
                    Timber.log(Log.DEBUG, "Response error: %s", responseWrapper.toString());
                    throw new IOException(String.format("Error getting response: %s", responseWrapper.errorBody()));
                }

                Timber.log(Log.DEBUG, "Finalizing signature...");
                containerWrapper.finalizeSignature(response.getSignature().getValue());
                Timber.log(Log.DEBUG, "Broadcasting create signature status response");
                broadcastMobileCreateSignatureStatusResponse(response, containerWrapper.getContainer());
                countDownLatch.countDown();
                return;
            }

            if (timeout > TIMEOUT_CANCEL) {
                Timber.log(Log.DEBUG, "Timeout: doCreateSignatureStatusRequestLoop timeout counter: %s", timeout);
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TIMEOUT));
                Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. Request timeout");
                return;
            }
            Timber.log(Log.DEBUG, "doCreateSignatureStatusRequestLoop timeout counter: %s", timeout);
            sleep(SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
            doCreateSignatureStatusRequestLoop(request);

    }

    private boolean isSessionStatusRequestComplete(MobileCreateSignatureSessionStatusResponse.ProcessState state) {
        return state.equals(MobileCreateSignatureSessionStatusResponse.ProcessState.COMPLETE);
    }

    private String getMobileIdSession(String hash, MobileCreateSignatureRequest request) throws IOException, SigningCancelledException {
        PostMobileCreateSignatureSessionRequest sessionRequest = getSessionRequest(request);
        Timber.log(Log.DEBUG, "Session request: %s", sessionRequest);
        sessionRequest.setHash(hash);
        Timber.log(Log.DEBUG, "Request hash: %s", hash);

        String requestString = MessageUtil.toJsonString(sessionRequest);
        Timber.log(Log.DEBUG, "Request string: %s", requestString);

            checkSigningCancelled(isCancelled);

            Call<MobileCreateSignatureSessionResponse> call = midRestServiceClient.getMobileCreateSession(requestString);

            MobileCreateSignatureSessionResponse sessionResponse;

            Response<MobileCreateSignatureSessionResponse> responseWrapper = call.execute();
            if (!responseWrapper.isSuccessful()) {
                Timber.log(Log.DEBUG, "Mobile-ID session request unsuccessful. Status: %s, message: %s, body: %s, errorBody: %s",
                        responseWrapper.code(), responseWrapper.message(), responseWrapper.body(), responseWrapper.errorBody());
                if (!isResponseError(responseWrapper, responseWrapper.body(), MobileCreateSignatureSessionResponse.class)) {
                    parseErrorAndBroadcast(responseWrapper);
                }
                return null;
            } else {
                sessionResponse = responseWrapper.body();
                Timber.log(Log.DEBUG, "Session response: %s", sessionResponse);
            }

            return sessionResponse != null ? sessionResponse.getSessionID() : null;
    }

    private void parseErrorAndBroadcast(Response responseWrapper) {
        if (responseWrapper.code() == 429) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS));
            Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. Too many requests, HTTP status code: %s", responseWrapper.code());
        } else if (responseWrapper.code() == 401) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
            Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. Invalid access rights, HTTP status code: %s", responseWrapper.code());
        } else if (responseWrapper.code() == 409) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS));
            Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. Exceeded unsuccessful requests, HTTP status code: %s", responseWrapper.code());
        } else {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TECHNICAL_ERROR));
            Timber.log(Log.DEBUG, "Failed to sign with Mobile-ID. Request unsuccessful, technical or general error, HTTP status code: %s", responseWrapper.code());
        }
    }

    private void broadcastFault(RESTServiceFault fault) {
        Timber.log(Log.DEBUG, "Broadcasting fault: %s", fault.toString());
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.SERVICE_FAULT)
                .putExtra(MobileSignConstants.SERVICE_FAULT, RESTServiceFault.toJson(fault));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureStatusResponse(MobileCreateSignatureSessionStatusResponse response, Container container) {
        Timber.log(Log.DEBUG, "Broadcasting create signature status response: %s", response.toString());
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_STATUS, MobileIdServiceResponse.toJson(generateMobileIdResponse(response, container)));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureResponse(String base64Hash) {
        Timber.log(Log.DEBUG, "Broadcasting create signature response: %s", base64Hash);
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_CHALLENGE, VerificationCodeUtil.calculateMobileIdVerificationCode(Base64.decode(base64Hash)));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private boolean isCountryCodeError(String phoneNumber) {
        return phoneNumber.length() <= 9;
    }

    private MobileIdServiceResponse generateMobileIdResponse(MobileCreateSignatureSessionStatusResponse response, Container container) {
        MobileIdServiceResponse mobileIdResponse = new MobileIdServiceResponse();
        mobileIdResponse.setContainer(container);
        mobileIdResponse.setStatus(response.getResult());
        mobileIdResponse.setSignature(response.getSignature().getValue());
        Timber.log(Log.DEBUG, "Mobile-ID status: %s, signature: %s", mobileIdResponse.getStatus().toString(), mobileIdResponse.getSignature());
        return mobileIdResponse;
    }

    private MobileCreateSignatureRequest getRequestFromData(String signatureRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MobileCreateSignatureRequest mobileCreateSignatureRequest = objectMapper.readValue(signatureRequest, MobileCreateSignatureRequest.class);
            Timber.log(Log.DEBUG, "Mobile-ID request from data: %s", mobileCreateSignatureRequest.toString());
            return mobileCreateSignatureRequest;
        } catch (JsonProcessingException e) {
            broadcastFault(defaultError(e.getMessage()));
            Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Failed to process signature request JSON. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        return null;
    }

    private RoleData getRoleDataFromData(String roleData) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            RoleData roleDataRequest = objectMapper.readValue(roleData, RoleData.class);
            Timber.log(Log.DEBUG, "Role data from data: %s", roleDataRequest != null ? roleDataRequest.toString() : "No role data");
            return roleDataRequest;
        } catch (JsonProcessingException e) {
            broadcastFault(defaultError(e.getMessage()));
            Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Failed to process role data request JSON. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        return null;
    }

    private PostMobileCreateSignatureCertificateRequest getCertificateRequest(MobileCreateSignatureRequest signatureRequest) {
        PostMobileCreateSignatureCertificateRequest certificateRequest = new PostMobileCreateSignatureCertificateRequest();
        certificateRequest.setRelyingPartyName(signatureRequest.getRelyingPartyName());
        certificateRequest.setRelyingPartyUUID(signatureRequest.getRelyingPartyUUID());
        certificateRequest.setPhoneNumber(signatureRequest.getPhoneNumber());
        certificateRequest.setNationalIdentityNumber(signatureRequest.getNationalIdentityNumber());
        Timber.log(Log.DEBUG, "Mobile-ID certificate request: %s", certificateRequest.toString());
        return certificateRequest;
    }

    private PostMobileCreateSignatureSessionRequest getSessionRequest(MobileCreateSignatureRequest signatureRequest) {
        PostMobileCreateSignatureSessionRequest sessionRequest = new PostMobileCreateSignatureSessionRequest();
        sessionRequest.setRelyingPartyUUID(signatureRequest.getRelyingPartyUUID());
        sessionRequest.setRelyingPartyName(signatureRequest.getRelyingPartyName());
        sessionRequest.setPhoneNumber(signatureRequest.getPhoneNumber());
        sessionRequest.setNationalIdentityNumber(signatureRequest.getNationalIdentityNumber());
        sessionRequest.setLanguage(signatureRequest.getLanguage());
        sessionRequest.setHashType(signatureRequest.getHashType());
        sessionRequest.setDisplayText(signatureRequest.getDisplayText());
        sessionRequest.setDisplayTextFormat(signatureRequest.getDisplayTextFormat());
        Timber.log(Log.DEBUG, "Mobile-ID session request: %s", sessionRequest.toString());
        return sessionRequest;
    }

    private <S> boolean isResponseError(Response<S> httpResponse, S response, Class<S> responseClass) {
        try {
            if (responseClass.equals(MobileCreateSignatureCertificateResponse.class)) {
                MobileCreateSignatureCertificateResponse certificateResponse = (MobileCreateSignatureCertificateResponse) response;
                if (!certificateResponse.getResult().equals(MobileCertificateResultType.OK)) {
                    RESTServiceFault fault = new RESTServiceFault(httpResponse.code(), certificateResponse.getResult(), certificateResponse.getTime(), certificateResponse.getTraceId(), certificateResponse.getError());
                    broadcastFault(fault);
                    Timber.log(Log.DEBUG, "Received Mobile-ID certificate response: %s", certificateResponse.toString());
                    return true;
                }
            } else if (responseClass.equals(MobileCreateSignatureSessionStatusResponse.class)) {
                MobileCreateSignatureSessionStatusResponse sessionStatusResponse = (MobileCreateSignatureSessionStatusResponse) response;
                if (!sessionStatusResponse.getResult().equals(MobileCreateSignatureSessionStatusResponse.ProcessStatus.OK)) {
                    RESTServiceFault restServiceFault = new RESTServiceFault(httpResponse.code(), sessionStatusResponse.getState(), sessionStatusResponse.getResult(), sessionStatusResponse.getTime(), sessionStatusResponse.getTraceId(), sessionStatusResponse.getError());
                    broadcastFault(restServiceFault);
                    Timber.log(Log.DEBUG, "Received Mobile-ID session signature response: %s", sessionStatusResponse.toString());
                    return true;
                }
            }
        } catch (ClassCastException e) {
            broadcastFault(defaultError(e.getMessage()));
            Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Unable to get correct response type. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            return true;
        }

        return false;
    }

    private RESTServiceFault defaultError(@Nullable String detailMessage) {
        Timber.log(Log.DEBUG, "Default error: %s", detailMessage != null ? detailMessage : "null");
        return new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.GENERAL_ERROR, detailMessage);
    }

    private void sleep(long millis) {
        try {
            timeout += millis;
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Timber.log(Log.ERROR, e, "Waiting for next call to MID REST API interrupted. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            Thread.currentThread().interrupt();
        }
    }
}
