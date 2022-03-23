/*
 * smart-id-lib
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

package ee.ria.DigiDoc.smartid.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import javax.net.ssl.SSLPeerUnverifiedException;

import ee.ria.DigiDoc.common.ContainerWrapper;
import ee.ria.DigiDoc.common.MessageUtil;
import ee.ria.DigiDoc.common.UUIDUtil;
import ee.ria.DigiDoc.common.VerificationCodeUtil;
import ee.ria.DigiDoc.smartid.dto.request.PostCertificateRequest;
import ee.ria.DigiDoc.smartid.dto.request.PostCreateSignatureRequest;
import ee.ria.DigiDoc.smartid.dto.request.SmartIDSignatureRequest;
import ee.ria.DigiDoc.smartid.dto.response.SessionResponse;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import ee.ria.DigiDoc.smartid.dto.response.SmartIDServiceResponse;
import ee.ria.DigiDoc.smartid.rest.SIDRestServiceClient;
import ee.ria.DigiDoc.smartid.rest.ServiceGenerator;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SmartSignService extends IntentService {

    private static final String PEM_BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END_CERT = "-----END CERTIFICATE-----";

    public static final String TAG = SmartSignService.class.getName();

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 1000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 5 * 1000;
    private static final long TIMEOUT_CANCEL = 80 * 1000;

    private SIDRestServiceClient SIDRestServiceClient;

    public SmartSignService() {
        super(TAG);
        Timber.tag(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.log(Log.DEBUG, "Handling smart sign intent");

        if (intent != null) {
            SmartIDSignatureRequest request =
                    (SmartIDSignatureRequest) intent.getSerializableExtra(SmartSignConstants.CREATE_SIGNATURE_REQUEST);

            ArrayList<String> certificateCertBundle = intent.getStringArrayListExtra(SmartSignConstants.CERTIFICATE_CERT_BUNDLE);

            if (request != null) {
                try {
                    if (certificateCertBundle != null) {
                        Timber.log(Log.DEBUG, request.toString());
                        SIDRestServiceClient = ServiceGenerator.createService(SIDRestServiceClient.class,
                                request.getUrl(), certificateCertBundle);
                    }
                } catch (CertificateException | NoSuchAlgorithmException e) {
                    broadcastFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE);
                    Timber.log(Log.ERROR, "SSL handshake failed. Exception message: %s.", e.getMessage());
                    return;
                }


                if (!UUIDUtil.isValid(request.getRelyingPartyUUID())) {
                    broadcastFault(SessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS);
                    Timber.log(Log.DEBUG, "%s - Relying Party UUID not in valid format", request.getRelyingPartyUUID());
                    return;
                }

                try {
                    SessionStatusResponse sessionStatusResponse = doSessionStatusRequestLoop(SIDRestServiceClient.getCertificate(
                            request.getCountry(), request.getNationalIdentityNumber(), getCertificateRequest(request)), true);
                    if (sessionStatusResponse == null) {
                        Timber.log(Log.ERROR, "No session status response");
                        return;
                    }

                    Timber.log(Log.DEBUG, sessionStatusResponse.toString());

                    ContainerWrapper containerWrapper = new ContainerWrapper(request.getContainerPath());
                    String base64Hash = containerWrapper.prepareSignature(getCertificatePem(sessionStatusResponse.getCert().getValue()));
                    if (base64Hash != null && !base64Hash.isEmpty()) {
                        broadcastSmartCreateSignatureChallengeResponse(base64Hash);
                        Thread.sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);

                        String requestString = MessageUtil.toJsonString(getSignatureRequest(request, base64Hash));

                        sessionStatusResponse = doSessionStatusRequestLoop(SIDRestServiceClient.getCreateSignature(
                                sessionStatusResponse.getResult().getDocumentNumber(), requestString), false);
                        if (sessionStatusResponse == null) {
                            Timber.log(Log.ERROR, "Unable to get session status response");
                            return;
                        }
                        Timber.log(Log.DEBUG, sessionStatusResponse.toString());
                        containerWrapper.finalizeSignature(sessionStatusResponse.getSignature().getValue());
                        broadcastSmartCreateSignatureStatusResponse(sessionStatusResponse);
                    }
                } catch (UnknownHostException e) {
                    broadcastFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE);
                    Timber.log(Log.ERROR, "REST API certificate request failed. Unknown host. Exception message: %s.", e.getMessage());
                } catch (SSLPeerUnverifiedException e) {
                    broadcastFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE);
                    Timber.log(Log.ERROR, "SSL handshake failed - Session status response. Exception message: %s.", e.getMessage());
                } catch (IOException e) {
                    broadcastFault();
                    Timber.log(Log.ERROR, "REST API certificate request failed. Exception message: %s.", e.getMessage());
                } catch (CertificateException e) {
                    broadcastFault();
                    Timber.log(Log.ERROR, "Generating certificate failed. Exception message: %s.", e.getMessage());
                } catch (InterruptedException e) {
                    Timber.log(Log.ERROR, "Waiting for next call to SID REST API interrupted. Exception message: %s.", e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (NoSuchAlgorithmException e) {
                    broadcastFault();
                    Timber.log(Log.ERROR, "Generating verification code failed. Exception message: %s.", e.getMessage());
                } catch (Exception e) {
                    Timber.log(Log.ERROR, "Exception message: %s.", e.getMessage());
                    if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                        broadcastFault(SessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS);
                        Timber.log(Log.ERROR, "Failed to sign with Smart-ID - Too Many Requests. Exception message: %s.", e.getMessage());
                    } else if (e.getMessage() != null && e.getMessage().contains("OCSP response not in valid time slot")) {
                        broadcastFault(SessionStatusResponse.ProcessStatus.OCSP_INVALID_TIME_SLOT);
                        Timber.log(Log.ERROR, "Failed to sign with Smart-ID - OCSP response not in valid time slot. Exception message: %s.", e.getMessage());
                    } else if (e.getMessage() != null && e.getMessage().contains("Certificate status: revoked")) {
                        broadcastFault(SessionStatusResponse.ProcessStatus.CERTIFICATE_REVOKED);
                        Timber.log(Log.ERROR, "Failed to sign with Smart-ID - Certificate status: revoked. Exception message: %s.", e.getMessage());
                    } else {
                        broadcastFault();
                        Timber.log(Log.ERROR, "Failed to sign with Smart-ID. Exception message: %s.", e.getMessage());
                    }
                }
            } else {
                Timber.log(Log.ERROR, "Invalid request");
                throw new IllegalStateException("Invalid request");
            }
        }
    }

    private String getCertificatePem(String cert) {
        return PEM_BEGIN_CERT + "\n" + cert + "\n" + PEM_END_CERT;
    }

    private SessionStatusResponse doSessionStatusRequestLoop(Call<SessionResponse> request, boolean certRequest) throws IOException {
        try {
            long timeout = 0;
            SessionResponse sessionResponse = handleRequest(request);
            if (sessionResponse == null) {
                Timber.log(Log.ERROR, "Session response null");
                return null;
            }
            Timber.log(Log.DEBUG, sessionResponse.toString());
            if (sessionResponse.getSessionID() == null || sessionResponse.getSessionID().isEmpty()) {
                broadcastFault(SessionStatusResponse.ProcessStatus.MISSING_SESSIONID);
                Timber.log(Log.DEBUG, "Received empty Smart-ID session response");
                return null;
            }

            while (timeout < TIMEOUT_CANCEL) {
                SessionStatusResponse sessionStatusResponse = handleRequest(SIDRestServiceClient.getSessionStatus(
                        sessionResponse.getSessionID(), SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS));
                if (sessionStatusResponse == null) {
                    Timber.log(Log.ERROR, "No session status response");
                    return null;
                }
                Timber.log(Log.DEBUG, sessionResponse.toString());
                if (sessionStatusResponse.getState().equals(SessionStatusResponse.ProcessState.COMPLETE)) {
                    SessionStatusResponse.ProcessStatus status = sessionStatusResponse.getResult().getEndResult();
                    if (status.equals(SessionStatusResponse.ProcessStatus.OK)) {
                        return sessionStatusResponse;
                    }
                    broadcastSmartCreateSignatureStatusResponse(sessionStatusResponse);
                    Timber.log(Log.DEBUG, "Received Smart-ID session status response: %s", status);
                    return null;
                }
                if (certRequest) {
                    broadcastSmartCreateSignatureSelectDevice();
                }
                timeout += SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS;
            }
            broadcastFault(SessionStatusResponse.ProcessStatus.ACCOUNT_NOT_FOUND_OR_TIMEOUT);
            Timber.log(Log.DEBUG, "Request timeout (ACCOUNT_NOT_FOUND_OR_TIMEOUT)");
        } catch (UnknownHostException e) {
            broadcastFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE);
            Timber.log(Log.ERROR, "REST API session status request failed. Unknown host. Exception message: %s.", e.getMessage());
        }
        return null;
    }

    private void broadcastFault() {
        Timber.log(Log.DEBUG, "Broadcasting fault: %s", SessionStatusResponse.ProcessStatus.GENERAL_ERROR);
        broadcastFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR);
    }

    private void broadcastFault(SessionStatusResponse.ProcessStatus status) {
        Timber.log(Log.DEBUG, "Broadcasting fault: %s", status);
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.SERVICE_FAULT)
                .putExtra(SmartSignConstants.SERVICE_FAULT, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureStatusResponse(SessionStatusResponse response) {
        Timber.log(Log.DEBUG, "broadcastSmartCreateSignatureStatusResponse: %s", response);
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(SmartSignConstants.CREATE_SIGNATURE_STATUS,
                        SmartIDServiceResponse.toJson(generateSmartIdResponse(response)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureSelectDevice() {
        Timber.log(Log.DEBUG, "User selecting device");
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_DEVICE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureChallengeResponse(String base64Hash) throws NoSuchAlgorithmException {
        Timber.log(Log.DEBUG, "Signature challenge");
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(SmartSignConstants.CREATE_SIGNATURE_CHALLENGE,
                        VerificationCodeUtil.calculateSmartIdVerificationCode(base64Hash));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private SmartIDServiceResponse generateSmartIdResponse(SessionStatusResponse response) {
        Timber.log(Log.DEBUG, "Generating Smart ID response: %s", response);
        SmartIDServiceResponse smartIdResponse = new SmartIDServiceResponse();
        smartIdResponse.setStatus(response.getResult().getEndResult());
        return smartIdResponse;
    }

    private PostCertificateRequest getCertificateRequest(SmartIDSignatureRequest request) {
        Timber.log(Log.DEBUG, "Certificate request: %s", request);
        PostCertificateRequest certificateRequest = new PostCertificateRequest();
        certificateRequest.setRelyingPartyName(request.getRelyingPartyName());
        certificateRequest.setRelyingPartyUUID(request.getRelyingPartyUUID());
        return certificateRequest;
    }

    private PostCreateSignatureRequest getSignatureRequest(
            SmartIDSignatureRequest request, String hash) {
        Timber.log(Log.DEBUG, "Signature request: %s", request);
        PostCreateSignatureRequest signatureRequest = new PostCreateSignatureRequest();
        signatureRequest.setRelyingPartyUUID(request.getRelyingPartyUUID());
        signatureRequest.setRelyingPartyName(request.getRelyingPartyName());
        signatureRequest.setHashType(request.getHashType());
        signatureRequest.setHash(hash);
        signatureRequest.setDisplayText(request.getDisplayText());
        signatureRequest.setRequestProperties(new PostCreateSignatureRequest.RequestProperties(true));
        return signatureRequest;
    }

    private <S> S handleRequest(Call<S> request) throws IOException {
        Response<S> httpResponse = request.execute();
        if (!httpResponse.isSuccessful()) {
            switch (httpResponse.code()) {
                case 401:
                case 403:
                    broadcastFault(SessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS);
                    Timber.log(Log.DEBUG, "Forbidden - HTTP status code: %s " + httpResponse.code());
                    break;
                case 404:
                    broadcastFault(httpResponse.body() instanceof SessionResponse ?
                            SessionStatusResponse.ProcessStatus.SESSION_NOT_FOUND :
                            SessionStatusResponse.ProcessStatus.ACCOUNT_NOT_FOUND_OR_TIMEOUT);
                    Timber.log(Log.DEBUG, "Account/session not found - HTTP status code: %s " + httpResponse.code());
                    break;
                case 409:
                    broadcastFault(SessionStatusResponse.ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS);
                    Timber.log(Log.DEBUG, "Exceeded unsuccessful requests - HTTP status code: %s " + httpResponse.code());
                    break;
                case 429:
                    broadcastFault(SessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS);
                    Timber.log(Log.DEBUG, "Too many requests - HTTP status code: %s " + httpResponse.code());
                    break;
                case 471:
                    broadcastFault(SessionStatusResponse.ProcessStatus.NOT_QUALIFIED);
                    Timber.log(Log.DEBUG, "Not qualified - HTTP status code: %s " + httpResponse.code());
                    break;
                case 480:
                    broadcastFault(SessionStatusResponse.ProcessStatus.OLD_API);
                    Timber.log(Log.DEBUG, "Old API - HTTP status code: %s " + httpResponse.code());
                    break;
                case 580:
                    broadcastFault(SessionStatusResponse.ProcessStatus.UNDER_MAINTENANCE);
                    Timber.log(Log.DEBUG, "Under maintenance - HTTP status code: %s " + httpResponse.code());
                    break;
                default:
                    broadcastFault();
                    Timber.log(Log.DEBUG, "Request unsuccessful, HTTP status code: %s " + httpResponse.code());
                    break;
            }
            return null;
        }
        Timber.log(Log.DEBUG, "Response body: %s", httpResponse.body());
        return httpResponse.body();
    }
}
