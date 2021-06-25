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

package ee.ria.DigiDoc.mobileid.service;

import android.app.IntentService;
import android.content.Intent;

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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;

import ee.ria.DigiDoc.common.ContainerWrapper;
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
        Timber.d("Handling mobile sign intent");
        timeout = 0;
        MobileCreateSignatureRequest request = getRequestFromIntent(intent);
        if (request != null) {
            PostMobileCreateSignatureCertificateRequest certificateRequest = getCertificateRequest(request);
            SSLContext restSSLConfig;
            try {
                restSSLConfig = createSSLConfig(intent);
            } catch (Exception e) {
                Timber.e(e, "Can't create SSL config");
                restSSLConfig = null;
            }

            try {
                ArrayList<String> certificateCertBundle = intent.getStringArrayListExtra(CERTIFICATE_CERT_BUNDLE);
                if (certificateCertBundle != null) {
                    midRestServiceClient = ServiceGenerator.createService(MIDRestServiceClient.class, restSSLConfig, request.getUrl(), certificateCertBundle);
                }
            } catch (CertificateException | NoSuchAlgorithmException e) {
                Timber.e(e, "Invalid SSL handshake");
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                return;
            }

            if (isCountryCodeError(request.getPhoneNumber())) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_COUNTRY_CODE));
                Timber.d("Invalid country code");
                return;
            }

            if (!UUIDUtil.isValid(request.getRelyingPartyUUID())) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
                Timber.d("%s - Relying Party UUID not in valid format", request.getRelyingPartyUUID());
                return;
            }

            Call<MobileCreateSignatureCertificateResponse> call = midRestServiceClient.getCertificate(certificateRequest);
            try {
                Response<MobileCreateSignatureCertificateResponse> responseWrapper = call.execute();
                if (!responseWrapper.isSuccessful()) {
                    parseErrorAndBroadcast(responseWrapper);
                } else {
                    MobileCreateSignatureCertificateResponse response = responseWrapper.body();
                    if (isResponseError(responseWrapper, response, MobileCreateSignatureCertificateResponse.class)) {
                        return;
                    }
                    containerWrapper = new ContainerWrapper(request.getContainerPath());
                    String base64Hash = containerWrapper.prepareSignature(getCertificatePem(response.getCert()));
                    if (base64Hash != null && !base64Hash.isEmpty()) {
                        broadcastMobileCreateSignatureResponse(base64Hash);
                        sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
                        String sessionId = getMobileIdSession(base64Hash, request);
                        if (sessionId == null) {
                            return;
                        }
                        doCreateSignatureStatusRequestLoop(new GetMobileCreateSignatureSessionStatusRequest(sessionId));
                    }
                }
            } catch (UnknownHostException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
                Timber.e(e, "REST API certificate request failed. Unknown host");
            } catch (SSLPeerUnverifiedException e) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                Timber.e(e, "SSL handshake failed");
            } catch (IOException e) {
                broadcastFault(defaultError());
                Timber.e(e, "REST API certificate request failed");
            } catch (CertificateException e) {
                broadcastFault(defaultError());
                Timber.e(e, "Generating certificate failed");
            }
        }
    }

    private String getCertificatePem(String cert) {
        return PEM_BEGIN_CERT + "\n" + cert + "\n" + PEM_END_CERT;
    }

    private static SSLContext createSSLConfig(Intent intent) throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        String keystorePath = intent.getStringExtra(ACCESS_TOKEN_PATH);
        String keystorePass = intent.getStringExtra(ACCESS_TOKEN_PASS);

        try (InputStream key = new FileInputStream(new File(keystorePath))) {
            String keyStoreType = "PKCS12";
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(key, keystorePass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, null);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        }
    }

    private void doCreateSignatureStatusRequestLoop(GetMobileCreateSignatureSessionStatusRequest request) throws IOException {
        Call<MobileCreateSignatureSessionStatusResponse> responseCall = midRestServiceClient.getMobileCreateSignatureSessionStatus(request.getSessionId(), request.getTimeoutMs());

        try {
            Response<MobileCreateSignatureSessionStatusResponse> responseWrapper = responseCall.execute();
            if (!responseWrapper.isSuccessful()) {
                parseErrorAndBroadcast(responseWrapper);
                return;
            }

            MobileCreateSignatureSessionStatusResponse response = responseWrapper.body();
            if (response != null && isSessionStatusRequestComplete(response.getState())) {
                if (isResponseError(responseWrapper, response, MobileCreateSignatureSessionStatusResponse.class)) {
                    return;
                }
                try {
                    containerWrapper.finalizeSignature(response.getSignature().getValue());
                    broadcastMobileCreateSignatureStatusResponse(response, containerWrapper.getContainer());
                    return;
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                        RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS);
                        broadcastFault(fault);
                        Timber.e(e, "Failed to sign with Mobile-ID - Too Many Requests");
                    } else if (e.getMessage() != null && e.getMessage().contains("OCSP response not in valid time slot")) {
                        RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.OCSP_INVALID_TIME_SLOT);
                        broadcastFault(fault);
                        Timber.e(e, "Failed to sign with Mobile-ID - OCSP response not in valid time slot");
                    } else if (e.getMessage() != null && e.getMessage().contains("Certificate status: revoked")) {
                        RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.CERTIFICATE_REVOKED);
                        broadcastFault(fault);
                        Timber.e(e, "Failed to sign with Mobile-ID - Certificate status: revoked");
                    } else {
                        RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.GENERAL_ERROR);
                        broadcastFault(fault);
                        Timber.e(e, "Failed to sign with Mobile-ID");
                    }
                    return;
                }
            }

            if (timeout > TIMEOUT_CANCEL) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TIMEOUT));
                Timber.d("Request timeout");
                return;
            }
            sleep(SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
            doCreateSignatureStatusRequestLoop(request);
        } catch (UnknownHostException e) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
            Timber.e(e, "REST API session status request failed. Unknown host");
        }
    }

    private boolean isSessionStatusRequestComplete(MobileCreateSignatureSessionStatusResponse.ProcessState state) {
        return state.equals(MobileCreateSignatureSessionStatusResponse.ProcessState.COMPLETE);
    }

    private String getMobileIdSession(String hash, MobileCreateSignatureRequest request) throws IOException {
        PostMobileCreateSignatureSessionRequest sessionRequest = getSessionRequest(request);
        sessionRequest.setHash(hash);

        Call<MobileCreateSignatureSessionResponse> call = midRestServiceClient.getMobileCreateSession(sessionRequest);

        MobileCreateSignatureSessionResponse sessionResponse;

        Response<MobileCreateSignatureSessionResponse> responseWrapper = call.execute();
        if (!responseWrapper.isSuccessful()) {
            if (!isResponseError(responseWrapper, responseWrapper.body(), MobileCreateSignatureSessionResponse.class)) {
                parseErrorAndBroadcast(responseWrapper);
            }
            return null;
        } else {
            sessionResponse = responseWrapper.body();
        }

        return sessionResponse != null ? sessionResponse.getSessionID() : null;
    }

    private void parseErrorAndBroadcast(Response responseWrapper) {
        if (responseWrapper.code() == 429) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS));
            Timber.d("Too many requests");
        } else if (responseWrapper.code() == 401) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
        } else if (responseWrapper.code() == 409) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS));
        } else {
            broadcastFault(defaultError());
            Timber.d("Request unsuccessful, HTTP status code: %s", responseWrapper.code());
        }
    }

    private void broadcastFault(RESTServiceFault fault) {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.SERVICE_FAULT)
                .putExtra(MobileSignConstants.SERVICE_FAULT, RESTServiceFault.toJson(fault));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureStatusResponse(MobileCreateSignatureSessionStatusResponse response, Container container) {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_STATUS, MobileIdServiceResponse.toJson(generateMobileIdResponse(response, container)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureResponse(String base64Hash) {
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
        return mobileIdResponse;
    }

    private MobileCreateSignatureRequest getRequestFromIntent(Intent intent) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(intent.getStringExtra(CREATE_SIGNATURE_REQUEST), MobileCreateSignatureRequest.class);
        } catch (JsonProcessingException e) {
            broadcastFault(defaultError());
            Timber.e(e, "Failed to process signature request JSON");
        }

        return null;

    }

    private PostMobileCreateSignatureCertificateRequest getCertificateRequest(MobileCreateSignatureRequest signatureRequest) {
        PostMobileCreateSignatureCertificateRequest certificateRequest = new PostMobileCreateSignatureCertificateRequest();
        certificateRequest.setRelyingPartyName(signatureRequest.getRelyingPartyName());
        certificateRequest.setRelyingPartyUUID(signatureRequest.getRelyingPartyUUID());
        certificateRequest.setPhoneNumber(signatureRequest.getPhoneNumber());
        certificateRequest.setNationalIdentityNumber(signatureRequest.getNationalIdentityNumber());

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

        return sessionRequest;
    }

    private <S> boolean isResponseError(Response<S> httpResponse, S response, Class<S> responseClass) {
        try {
            if (responseClass.equals(MobileCreateSignatureCertificateResponse.class)) {
                MobileCreateSignatureCertificateResponse certificateResponse = (MobileCreateSignatureCertificateResponse) response;
                if (!certificateResponse.getResult().equals(MobileCertificateResultType.OK)) {
                    RESTServiceFault fault = new RESTServiceFault(httpResponse.code(), certificateResponse.getResult(), certificateResponse.getTime(), certificateResponse.getTraceId(), certificateResponse.getError());
                    broadcastFault(fault);
                    Timber.d("Received Mobile-ID certificate response: %s", certificateResponse.getResult());
                    return true;
                }
            } else if (responseClass.equals(MobileCreateSignatureSessionStatusResponse.class)) {
                MobileCreateSignatureSessionStatusResponse sessionStatusResponse = (MobileCreateSignatureSessionStatusResponse) response;
                if (!sessionStatusResponse.getResult().equals(MobileCreateSignatureSessionStatusResponse.ProcessStatus.OK)) {
                    RESTServiceFault restServiceFault = new RESTServiceFault(httpResponse.code(), sessionStatusResponse.getState(), sessionStatusResponse.getResult(), sessionStatusResponse.getTime(), sessionStatusResponse.getTraceId(), sessionStatusResponse.getError());
                    broadcastFault(restServiceFault);
                    Timber.d("Received Mobile-ID session signature response: %s", sessionStatusResponse.getResult());
                    return true;
                }
            }
        } catch (ClassCastException e) {
            broadcastFault(defaultError());
            Timber.e(e, "Unable to get correct response type");
            return true;
        }

        return false;
    }

    private RESTServiceFault defaultError() {
        return new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.GENERAL_ERROR);
    }

    private void sleep(long millis) {
        try {
            timeout += millis;
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Timber.e(e, "Waiting for next call to MID REST API interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
