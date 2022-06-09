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

package ee.ria.DigiDoc.mobileid.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;

import ee.ria.DigiDoc.common.ContainerWrapper;
import ee.ria.DigiDoc.common.MessageUtil;
import ee.ria.DigiDoc.common.TrustManagerUtil;
import ee.ria.DigiDoc.common.UUIDUtil;
import ee.ria.DigiDoc.common.VerificationCodeUtil;
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

import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;

public class MobileSignService extends IntentService {

    private static final String PEM_BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END_CERT = "-----END CERTIFICATE-----";

    public static final String TAG = MobileSignService.class.getName();

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 1000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 5 * 1000;
    private static final long TIMEOUT_CANCEL = 120 * 1000;
    private long timeout;

    private ContainerWrapper containerWrapper;

    private MIDRestServiceClient midRestServiceClient;

    public MobileSignService() {
        super(TAG);
        Timber.tag(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.log(Log.DEBUG, "Handling mobile sign intent");

        TrustManager[] trustManagers = new TrustManager[0];
        try {
            trustManagers = TrustManagerUtil.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            Timber.log(Log.ERROR, e, "Unable to get Trust Managers. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        timeout = 0;
        MobileCreateSignatureRequest request = getRequestFromIntent(intent);
        if (request != null) {
            PostMobileCreateSignatureCertificateRequest certificateRequest = getCertificateRequest(request);
            Timber.log(Log.DEBUG, "Certificate request: %s", certificateRequest.toString());
            SSLContext restSSLConfig;
            try {
                Timber.log(Log.DEBUG, "Creating SSL config");
                restSSLConfig = createSSLConfig(intent, trustManagers);
            } catch (Exception e) {
                Timber.log(Log.ERROR, e, "Can't create SSL config. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                restSSLConfig = null;
            }

            try {
                ArrayList<String> certificateCertBundle = intent.getStringArrayListExtra(CERTIFICATE_CERT_BUNDLE);
                if (certificateCertBundle != null) {
                    midRestServiceClient = ServiceGenerator.createService(MIDRestServiceClient.class,
                            restSSLConfig, request.getUrl(), certificateCertBundle, trustManagers, getApplicationContext());
                } else {
                    Timber.log(Log.DEBUG, "Certificate cert bundle is null");
                }
            } catch (CertificateException | NoSuchAlgorithmException e) {
                Timber.log(Log.ERROR, e, "Invalid SSL handshake. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                return;
            }

            if (isCountryCodeError(request.getPhoneNumber())) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_COUNTRY_CODE));
                Timber.log(Log.DEBUG, "Invalid country code");
                return;
            }

            if (!UUIDUtil.isValid(request.getRelyingPartyUUID())) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
                Timber.log(Log.DEBUG, "%s - Relying Party UUID not in valid format", request.getRelyingPartyUUID());
                return;
            }

            Call<MobileCreateSignatureCertificateResponse> call = midRestServiceClient.getCertificate(certificateRequest);
            try {
                Response<MobileCreateSignatureCertificateResponse> responseWrapper = call.execute();
                if (!responseWrapper.isSuccessful()) {
                    Timber.log(Log.DEBUG, "Mobile-ID certificate request unsuccessful. Status: %s, message: %s, body: %s, errorBody: %s",
                            responseWrapper.code(), responseWrapper.message(), responseWrapper.body(), responseWrapper.errorBody());
                    parseErrorAndBroadcast(responseWrapper);
                } else {
                    MobileCreateSignatureCertificateResponse response = responseWrapper.body();
                    Timber.log(Log.DEBUG, "MobileCreateSignatureCertificateResponse response body: %s", response != null ? response.toString() : "null");
                    if (isResponseError(responseWrapper, response, MobileCreateSignatureCertificateResponse.class)) {
                        return;
                    }
                    containerWrapper = new ContainerWrapper(request.getContainerPath());
                    String base64Hash = containerWrapper.prepareSignature(getCertificatePem(response.getCert()));
                    if (base64Hash != null && !base64Hash.isEmpty()) {
                        Timber.log(Log.DEBUG, "Broadcasting create signature response");
                        broadcastMobileCreateSignatureResponse(base64Hash);
                        sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
                        String sessionId = getMobileIdSession(base64Hash, request);
                        if (sessionId == null) {
                            Timber.log(Log.DEBUG, "Session ID missing");
                            return;
                        }
                        Timber.log(Log.DEBUG, "Session ID: %s", sessionId);
                        doCreateSignatureStatusRequestLoop(new GetMobileCreateSignatureSessionStatusRequest(sessionId));
                    } else {
                        Timber.log(Log.DEBUG, "Base64 (Prepare signature) is empty or null");
                    }
                }
            } catch (UnknownHostException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
                Timber.log(Log.ERROR, e, "REST API certificate request failed. Unknown host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            } catch (SSLPeerUnverifiedException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                Timber.log(Log.ERROR, e, "SSL handshake failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            } catch (IOException e) {
                broadcastFault(defaultError(e.getMessage()));
                Timber.log(Log.ERROR, e, "REST API certificate request failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            } catch (CertificateException e) {
                broadcastFault(defaultError(e.getMessage()));
                Timber.log(Log.ERROR, e, "Generating certificate failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            } catch (Exception e) {
                broadcastFault(defaultError(e.getMessage()));
                Timber.log(Log.ERROR, e, "Failed to get certificate or parse response. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private String getCertificatePem(String cert) {
        return PEM_BEGIN_CERT + "\n" + cert + "\n" + PEM_END_CERT;
    }

    private static SSLContext createSSLConfig(Intent intent, TrustManager[] trustManagers) throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        String keystorePath = intent.getStringExtra(ACCESS_TOKEN_PATH);
        String keystorePass = intent.getStringExtra(ACCESS_TOKEN_PASS);

        try (InputStream key = new FileInputStream(new File(keystorePath))) {
            String keyStoreType = "PKCS12";
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(key, keystorePass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, null);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.createSSLEngine().setEnabledProtocols(new String [] { "TLSv1.2", "TLSv1.3" });
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
            return sslContext;
        }
    }

    private void doCreateSignatureStatusRequestLoop(GetMobileCreateSignatureSessionStatusRequest request) throws IOException {
        Call<MobileCreateSignatureSessionStatusResponse> responseCall = midRestServiceClient.getMobileCreateSignatureSessionStatus(request.getSessionId(), request.getTimeoutMs());

        try {
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
                    return;
                }
                try {
                    Timber.log(Log.DEBUG, "Finalizing signature...");
                    containerWrapper.finalizeSignature(response.getSignature().getValue());
                    Timber.log(Log.DEBUG, "Broadcasting create signature status response");
                    broadcastMobileCreateSignatureStatusResponse(response, containerWrapper.getContainer());
                    return;
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
                    } else {
                        RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TECHNICAL_ERROR);
                        broadcastFault(fault);
                        Timber.log(Log.ERROR, e, "Failed to sign with Mobile-ID. Technical or general error %s. Exception message: %s. Exception: %s", responseWrapper.code(), e.getMessage(), Arrays.toString(e.getStackTrace()));
                    }
                    return;
                }
            }

            if (timeout > TIMEOUT_CANCEL) {
                Timber.log(Log.DEBUG, "Timeout: doCreateSignatureStatusRequestLoop timeout counter: %s", timeout);
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TIMEOUT));
                Timber.log(Log.DEBUG, "Request timeout");
                return;
            }
            Timber.log(Log.DEBUG, "doCreateSignatureStatusRequestLoop timeout counter: %s", timeout);
            sleep(SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
            doCreateSignatureStatusRequestLoop(request);
        } catch (UnknownHostException e) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
            Timber.log(Log.ERROR, e, "REST API session status request failed. Unknown host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    private boolean isSessionStatusRequestComplete(MobileCreateSignatureSessionStatusResponse.ProcessState state) {
        return state.equals(MobileCreateSignatureSessionStatusResponse.ProcessState.COMPLETE);
    }

    private String getMobileIdSession(String hash, MobileCreateSignatureRequest request) throws IOException {
        PostMobileCreateSignatureSessionRequest sessionRequest = getSessionRequest(request);
        Timber.log(Log.DEBUG, "Session request: %s", sessionRequest);
        sessionRequest.setHash(hash);
        Timber.log(Log.DEBUG, "Request hash: %s", hash);

        String requestString = MessageUtil.toJsonString(sessionRequest);
        Timber.log(Log.DEBUG, "Request string: %s", requestString);

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
            Timber.log(Log.DEBUG, "Too many requests, HTTP status code: %s", responseWrapper.code());
        } else if (responseWrapper.code() == 401) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
            Timber.log(Log.DEBUG, "Too many requests, HTTP status code: %s", responseWrapper.code());
        } else if (responseWrapper.code() == 409) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS));
            Timber.log(Log.DEBUG, "Exceeded unsuccessful requests, HTTP status code: %s", responseWrapper.code());
        } else {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TECHNICAL_ERROR));
            Timber.log(Log.DEBUG, "Request unsuccessful, technical or general error, HTTP status code: %s", responseWrapper.code());
        }
    }

    private void broadcastFault(RESTServiceFault fault) {
        Timber.log(Log.DEBUG, "Broadcasting fault: %s", fault.toString());
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.SERVICE_FAULT)
                .putExtra(MobileSignConstants.SERVICE_FAULT, RESTServiceFault.toJson(fault));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureStatusResponse(MobileCreateSignatureSessionStatusResponse response, Container container) {
        Timber.log(Log.DEBUG, "Broadcasting create signature status response: %s", response.toString());
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_STATUS, MobileIdServiceResponse.toJson(generateMobileIdResponse(response, container)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureResponse(String base64Hash) {
        Timber.log(Log.DEBUG, "Broadcasting create signature response: %s", base64Hash);
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_CHALLENGE, VerificationCodeUtil.calculateMobileIdVerificationCode(Base64.decode(base64Hash)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
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

    private MobileCreateSignatureRequest getRequestFromIntent(Intent intent) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MobileCreateSignatureRequest mobileCreateSignatureRequest = objectMapper.readValue(intent.getStringExtra(CREATE_SIGNATURE_REQUEST), MobileCreateSignatureRequest.class);
            Timber.log(Log.DEBUG, "Mobile-ID request from intent: %s", mobileCreateSignatureRequest.toString());
            return mobileCreateSignatureRequest;
        } catch (JsonProcessingException e) {
            broadcastFault(defaultError(e.getMessage()));
            Timber.log(Log.ERROR, e, "Failed to process signature request JSON. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
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
            Timber.log(Log.ERROR, e, "Unable to get correct response type. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
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
