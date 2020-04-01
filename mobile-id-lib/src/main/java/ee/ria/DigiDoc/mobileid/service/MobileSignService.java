/*
 * Copyright 2020 Riigi Infosüsteemide Amet
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
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType;
import ee.ria.DigiDoc.mobileid.dto.request.GetMobileCreateSignatureSessionStatusRequest;
import ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.DigiDoc.mobileid.dto.request.PostMobileCreateSignatureCertificateRequest;
import ee.ria.DigiDoc.mobileid.dto.request.PostMobileCreateSignatureSessionRequest;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureCertificateResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileIdResponse;
import ee.ria.DigiDoc.mobileid.dto.response.RESTServiceFault;
import ee.ria.DigiDoc.mobileid.rest.ContainerActions;
import ee.ria.DigiDoc.mobileid.rest.MIDRestServiceClient;
import ee.ria.DigiDoc.mobileid.rest.ServiceGenerator;
import ee.ria.libdigidocpp.Container;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SIGN_SERVICE_URL;

public class MobileSignService extends IntentService {

    public static final String TAG = MobileSignService.class.getName();

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 1000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 1000;
    private static final long TIMEOUT_CANCEL = 120 * 1000;
    private long timeout;

    private ContainerActions containerActions;

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
        PostMobileCreateSignatureCertificateRequest certificateRequest = getCertificateRequest(request);
        SSLContext restSSLConfig;
        try {
            restSSLConfig = createSSLConfig(intent);
        } catch (Exception e) {
            Timber.e(e, "Can't create SSL config");
            restSSLConfig = null;
        }
        midRestServiceClient = ServiceGenerator.createService(MIDRestServiceClient.class, restSSLConfig, intent.getStringExtra(SIGN_SERVICE_URL));

        if (isCountryCodeError(request.getPhoneNumber())) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.INVALID_COUNTRY_CODE));
            Timber.d("Invalid country code");
            return;
        }

        Call<MobileCreateSignatureCertificateResponse> call = midRestServiceClient.getCertificate(certificateRequest);
        System.out.println("GETTING CERTIFICATE RESPONSE...");
        try {
            Response<MobileCreateSignatureCertificateResponse> responseWrapper = call.execute();
            if (!responseWrapper.isSuccessful()) {
                parseErrorAndBroadcast(responseWrapper);
            } else {
                MobileCreateSignatureCertificateResponse response = responseWrapper.body();
                if (isResponseError(responseWrapper, response, MobileCreateSignatureCertificateResponse.class)) {
                    return;
                }
                System.out.println("GENERATING HASH...");
                containerActions = new ContainerActions(request.getContainerPath(), response.getCert());
                String hash = generateHash();
                broadcastMobileCreateSignatureResponse();
                sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
                System.out.println("GETTING SESSION ID...");
                String sessionId = getMobileIdSession(hash, request);
                doCreateSignatureStatusRequestLoop(new GetMobileCreateSignatureSessionStatusRequest(sessionId));
            }
        } catch (UnknownHostException e) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
            Timber.e(e, "REST API certificate request failed. Unknown host");
        } catch (IOException e) {
            broadcastFault(defaultError());
            Timber.e(e, "REST API certificate request failed");
        } catch (CertificateException e) {
            broadcastFault(defaultError());
            Timber.e(e, "Generating certificate failed");
        }
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
        System.out.println("Session status request: " + responseCall.request().toString());
        System.out.println("GETTING RESPONSE...");
        try {
            Response<MobileCreateSignatureSessionStatusResponse> responseWrapper = responseCall.execute();
            System.out.println("GOT RESPONSE");
            if (!responseWrapper.isSuccessful()) {
                parseErrorAndBroadcast(responseWrapper);
                return;
            }

            MobileCreateSignatureSessionStatusResponse response = responseWrapper.body();
            System.out.println("SESSION STATUS RESULT: " + response.getResult());
            System.out.println("Session status response:" + response.toString());
            if (response != null && isSessionStatusRequestComplete(response.getState())) {
                if (isResponseError(responseWrapper, response, MobileCreateSignatureSessionStatusResponse.class)) {
                    containerActions.removeSignatureFromContainer();
                    return;
                }
                try {
                    System.out.println("REQUEST COMPLETE");
                    System.out.println("VALIDATING SIGNATURE");
                    if (containerActions.validateSignature(response.getSignature().getValue())) {
                        System.out.println("SIGNATURE VALIDATED");
                        broadcastMobileCreateSignatureStatusResponse(response, containerActions.getContainer());
                    } else {
                        containerActions.removeSignatureFromContainer();
                        broadcastFault(defaultError());
                        Timber.e("Signature validation failed");
                    }
                    return;
                } catch (Exception e) {
                    RESTServiceFault fault = new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.GENERAL_ERROR);
                    broadcastFault(fault);
                    Timber.e(e, "Unable to validate signature");
                }
            }

            if (timeout > TIMEOUT_CANCEL) {
                broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TIMEOUT));
                System.out.println("REQUEST TIMEOUT");
                Timber.d("Request timeout");
                return;
            }
            System.out.println("MAKING A NEW REQUEST");
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

    private String generateHash() throws CertificateException {
        return containerActions.generateHash();
    }

    private String getMobileIdSession(String hash, MobileCreateSignatureRequest request) {
        PostMobileCreateSignatureSessionRequest sessionRequest = getSessionRequest(request);
        sessionRequest.setHash(hash);

        Call<MobileCreateSignatureSessionResponse> call = midRestServiceClient.getMobileCreateSession(sessionRequest);

        MobileCreateSignatureSessionResponse sessionResponse = new MobileCreateSignatureSessionResponse();

        try {
            Response<MobileCreateSignatureSessionResponse> responseWrapper = call.execute();
            if (!responseWrapper.isSuccessful()) {
                if (isResponseError(responseWrapper, null, MobileCreateSignatureSessionResponse.class)) {
                    containerActions.removeSignatureFromContainer();
                    return "";
                }

                parseErrorAndBroadcast(responseWrapper);
            } else {
                sessionResponse = responseWrapper.body();
            }
        } catch (UnknownHostException e) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.NO_RESPONSE));
            Timber.e(e, "REST API session request failed. Unknown host");
        } catch (IOException e) {
            broadcastFault(defaultError());
            Timber.e(e, "REST API request failed");
            return "";
        }

        return sessionResponse.getSessionID();
    }

    private void parseErrorAndBroadcast(Response responseWrapper) {
        if (responseWrapper.code() == 429) {
            broadcastFault(new RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS));
            Timber.d("Too many requests");
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
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_STATUS, MobileIdResponse.toJson(generateMobileIdResponse(response, container)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureResponse() {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_CHALLENGE, ContainerActions.calculateMobileIdVerificationCode(
                        containerActions.getDataToSign()
                ));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private boolean isCountryCodeError(String phoneNumber) {
        return phoneNumber.length() <= 9;
    }

    private MobileIdResponse generateMobileIdResponse(MobileCreateSignatureSessionStatusResponse response, Container container) {
        MobileIdResponse mobileIdResponse = new MobileIdResponse();
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
