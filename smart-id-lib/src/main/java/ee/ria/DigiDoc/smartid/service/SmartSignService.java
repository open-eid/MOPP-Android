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

package ee.ria.DigiDoc.smartid.service;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLPeerUnverifiedException;

import ee.ria.DigiDoc.common.ContainerWrapper;
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
    private static final long TIMEOUT_CANCEL = 120 * 1000;

    private SIDRestServiceClient SIDRestServiceClient;

    public SmartSignService() {
        super(TAG);
        Timber.tag(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("Handling smart sign intent");
        SmartIDSignatureRequest request =
                (SmartIDSignatureRequest) intent.getSerializableExtra(SmartSignConstants.CREATE_SIGNATURE_REQUEST);

        try {
            SIDRestServiceClient = ServiceGenerator.createService(SIDRestServiceClient.class,
                    request.getUrl(), intent.getStringArrayListExtra(SmartSignConstants.CERTIFICATE_CERT_BUNDLE));
        } catch (CertificateException | NoSuchAlgorithmException e) {
            broadcastFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE);
            Timber.e(e, "SSL handshake failed");
            return;
        }

        try {
            SessionStatusResponse sessionStatusResponse = doSessionStatusRequestLoop(SIDRestServiceClient.getCertificate(
                    request.getCountry(), request.getNationalIdentityNumber(), getCertificateRequest(request)), true);
            if (sessionStatusResponse == null) {
                return;
            }

            ContainerWrapper containerWrapper = new ContainerWrapper(request.getContainerPath());
            String base64Hash = containerWrapper.prepareSignature(getCertificatePem(sessionStatusResponse.getCert().getValue()));
            broadcastSmartCreateSignatureChallengeResponse(base64Hash);
            Thread.sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);

            sessionStatusResponse = doSessionStatusRequestLoop(SIDRestServiceClient.getCreateSignature(
                    sessionStatusResponse.getResult().getDocumentNumber(), getSignatureRequest(request, base64Hash)), false);
            if (sessionStatusResponse == null) {
                return;
            }
            containerWrapper.finalizeSignature(sessionStatusResponse.getSignature().getValue());
            broadcastSmartCreateSignatureStatusResponse(sessionStatusResponse);
        } catch (UnknownHostException e) {
            broadcastFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE);
            Timber.e(e, "REST API certificate request failed. Unknown host");
        } catch (SSLPeerUnverifiedException e) {
            broadcastFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE);
            Timber.e(e, "SSL handshake failed");
        } catch (IOException e) {
            broadcastFault();
            Timber.e(e, "REST API certificate request failed");
        } catch (CertificateException e) {
            broadcastFault();
            Timber.e(e, "Generating certificate failed");
        } catch (InterruptedException e) {
            Timber.e(e, "Waiting for next call to SID REST API interrupted");
            Thread.currentThread().interrupt();
        } catch (NoSuchAlgorithmException e) {
            broadcastFault();
            Timber.e(e, "Generating verification code failed");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                broadcastFault(SessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS);
                Timber.e(e, "Failed to sign with Smart-ID - Too Many Requests");
            } else if (e.getMessage() != null && e.getMessage().contains("OCSP response not in valid time slot")) {
                broadcastFault(SessionStatusResponse.ProcessStatus.OCSP_INVALID_TIME_SLOT);
                Timber.e(e, "Failed to sign with Smart-ID - OCSP response not in valid time slot");
            } else {
                broadcastFault();
                Timber.e(e, "Failed to sign with Smart-ID");
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
                return null;
            }
            if (sessionResponse.getSessionID() == null || sessionResponse.getSessionID().isEmpty()) {
                broadcastFault(SessionStatusResponse.ProcessStatus.MISSING_SESSIONID);
                Timber.d("Received empty Smart-ID session response");
                return null;
            }

            while (timeout < TIMEOUT_CANCEL) {
                SessionStatusResponse sessionStatusResponse = handleRequest(SIDRestServiceClient.getSessionStatus(
                        sessionResponse.getSessionID(), SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS));
                if (sessionStatusResponse == null) {
                    return null;
                }
                if (sessionStatusResponse.getState().equals(SessionStatusResponse.ProcessState.COMPLETE)) {
                    SessionStatusResponse.ProcessStatus status = sessionStatusResponse.getResult().getEndResult();
                    if (status.equals(SessionStatusResponse.ProcessStatus.OK)) {
                        return sessionStatusResponse;
                    }
                    broadcastFault(status);
                    Timber.d("Received Smart-ID session status response: %s", status);
                    return null;
                }
                if (certRequest) {
                    broadcastSmartCreateSignatureSelectDevice();
                }
                timeout += SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS;
            }
            broadcastFault(SessionStatusResponse.ProcessStatus.TIMEOUT);
            Timber.d("Request timeout");
        } catch (UnknownHostException e) {
            broadcastFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE);
            Timber.e(e, "REST API session status request failed. Unknown host");
        }
        return null;
    }

    private void broadcastFault() {
        broadcastFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR);
    }

    private void broadcastFault(SessionStatusResponse.ProcessStatus status) {
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.SERVICE_FAULT)
                .putExtra(SmartSignConstants.SERVICE_FAULT, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureStatusResponse(SessionStatusResponse response) {
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(SmartSignConstants.CREATE_SIGNATURE_STATUS,
                        SmartIDServiceResponse.toJson(generateSmartIdResponse(response)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureSelectDevice() {
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_DEVICE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureChallengeResponse(String base64Hash) throws NoSuchAlgorithmException {
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(SmartSignConstants.CREATE_SIGNATURE_CHALLENGE,
                        VerificationCodeUtil.calculateSmartIdVerificationCode(base64Hash));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private SmartIDServiceResponse generateSmartIdResponse(SessionStatusResponse response) {
        SmartIDServiceResponse smartIdResponse = new SmartIDServiceResponse();
        smartIdResponse.setStatus(response.getResult().getEndResult());
        return smartIdResponse;
    }

    private PostCertificateRequest getCertificateRequest(SmartIDSignatureRequest request) {
        PostCertificateRequest certificateRequest = new PostCertificateRequest();
        certificateRequest.setRelyingPartyName(request.getRelyingPartyName());
        certificateRequest.setRelyingPartyUUID(request.getRelyingPartyUUID());
        return certificateRequest;
    }

    private PostCreateSignatureRequest getSignatureRequest(
            SmartIDSignatureRequest request, String hash) {
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
                    broadcastFault(SessionStatusResponse.ProcessStatus.FORBIDDEN);
                    Timber.d("Forbidden");
                    break;
                case 404:
                    broadcastFault(httpResponse.body() instanceof SessionResponse ?
                            SessionStatusResponse.ProcessStatus.SESSION_NOT_FOUND :
                            SessionStatusResponse.ProcessStatus.ACCOUNT_NOT_FOUND);
                    Timber.d("Account/session not found");
                    break;
                case 429:
                    broadcastFault(SessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS);
                    Timber.d("Too many requests");
                    break;
                case 471:
                    broadcastFault(SessionStatusResponse.ProcessStatus.NOT_QUALIFIED);
                    Timber.d("Not qualified");
                    break;
                case 480:
                    broadcastFault(SessionStatusResponse.ProcessStatus.OLD_API);
                    Timber.d("Old API");
                    break;
                case 580:
                    broadcastFault(SessionStatusResponse.ProcessStatus.UNDER_MAINTENANCE);
                    Timber.d("Under maintenance");
                    break;
                default:
                    broadcastFault();
                    Timber.d("Request unsuccessful, HTTP status code: %s", httpResponse.code());
                    break;
            }
            return null;
        }
        return httpResponse.body();
    }
}
