/*
 * smart-id-lib
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

package ee.ria.DigiDoc.smartid.service;

import static ee.ria.DigiDoc.common.ProxySetting.NO_PROXY;
import static ee.ria.DigiDoc.common.SigningUtil.checkSigningCancelled;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.MANUAL_PROXY_HOST;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.MANUAL_PROXY_PASSWORD;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.MANUAL_PROXY_PORT;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.MANUAL_PROXY_USERNAME;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.NOTIFICATION_CHANNEL;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.NOTIFICATION_PERMISSION_CODE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.PROXY_SETTING;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SIGNING_ROLE_DATA;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import ee.ria.DigiDoc.common.ContainerWrapper;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.MessageUtil;
import ee.ria.DigiDoc.common.NotificationUtil;
import ee.ria.DigiDoc.common.PowerUtil;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.ProxyUtil;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.common.UUIDUtil;
import ee.ria.DigiDoc.common.VerificationCodeUtil;
import ee.ria.DigiDoc.common.exception.SigningCancelledException;
import ee.ria.DigiDoc.smartid.R;
import ee.ria.DigiDoc.smartid.dto.request.PostCertificateRequest;
import ee.ria.DigiDoc.smartid.dto.request.PostCreateSignatureRequestV2;
import ee.ria.DigiDoc.smartid.dto.request.RequestAllowedInteractionsOrder;
import ee.ria.DigiDoc.smartid.dto.request.SmartIDSignatureRequest;
import ee.ria.DigiDoc.smartid.dto.response.ServiceFault;
import ee.ria.DigiDoc.smartid.dto.response.SessionResponse;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import ee.ria.DigiDoc.smartid.dto.response.SmartIDServiceResponse;
import ee.ria.DigiDoc.smartid.rest.SIDRestServiceClient;
import ee.ria.DigiDoc.smartid.rest.ServiceGenerator;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SmartSignService extends Worker {

    private static final String NOTIFICATION_NAME = "Smart-ID";
    private static final String PEM_BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END_CERT = "-----END CERTIFICATE-----";

    public static final String TAG = SmartSignService.class.getName();

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 1000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 5 * 1000;
    private static final long TIMEOUT_CANCEL = 80 * 1000;
    private static boolean isCancelled = false;
    private final String roleData;

    private SIDRestServiceClient SIDRestServiceClient;

    private final String signatureRequest;
    private final ArrayList<String> certificateCertBundle;

    private final ProxySetting proxySetting;
    private final ManualProxy manualProxySettings;

    public SmartSignService(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
        Timber.tag(TAG);

        isCancelled = false;

        roleData = workerParameters.getInputData().getString(SIGNING_ROLE_DATA);

        signatureRequest = workerParameters.getInputData().getString(CREATE_SIGNATURE_REQUEST);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String certBundleList = sharedPreferences.getString(CERTIFICATE_CERT_BUNDLE, "");

        Type arraylistType = new TypeToken<ArrayList<String>>() {}.getType();
        certificateCertBundle = new Gson().fromJson(certBundleList, arraylistType);

        try {
            proxySetting = ProxySetting.valueOf(workerParameters.getInputData().getString(PROXY_SETTING));
        } catch (IllegalArgumentException iae) {
            Timber.log(Log.ERROR, iae, "Proxy setting cannot be empty");
            broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR));
            throw iae;
        }

        manualProxySettings = new ManualProxy(
                workerParameters.getInputData().getString(MANUAL_PROXY_HOST),
                workerParameters.getInputData().getInt(MANUAL_PROXY_PORT, 0),
                workerParameters.getInputData().getString(MANUAL_PROXY_USERNAME),
                workerParameters.getInputData().getString(MANUAL_PROXY_PASSWORD)
        );
    }

    public static void setIsCancelled(boolean isCancelled) {
        SmartSignService.isCancelled = isCancelled;
    }

    private void createNotificationChannel() {
        Timber.log(Log.DEBUG, "Creating notification channel");
        NotificationUtil.createNotificationChannel(getApplicationContext(), NOTIFICATION_CHANNEL, NOTIFICATION_NAME);
    }

    public void showEmptyNotification() {
        createNotificationChannel();
        Notification notification = NotificationUtil.createNotification(getApplicationContext(), NOTIFICATION_CHANNEL,
                R.mipmap.ic_launcher, null, null, NotificationCompat.PRIORITY_MIN, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setForegroundAsync(new ForegroundInfo(NOTIFICATION_PERMISSION_CODE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE));
        } else {
            setForegroundAsync(new ForegroundInfo(NOTIFICATION_PERMISSION_CODE, notification));
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (PowerUtil.isPowerSavingMode(getApplicationContext())) {
            showEmptyNotification();
        }

        Timber.log(Log.DEBUG, "Handling smart sign intent");

        SmartIDSignatureRequest request = getRequestFromData(signatureRequest);
        RoleData roleDataRequest = getRoleDataFromData(roleData);
            if (request != null) {
                try {
                    if (certificateCertBundle != null) {
                        Timber.log(Log.DEBUG, request.toString());
                        SIDRestServiceClient = ServiceGenerator.createService(SIDRestServiceClient.class,
                                request.getUrl() + "/", certificateCertBundle,
                                proxySetting, manualProxySettings, getApplicationContext());
                    }
                } catch (CertificateException | NoSuchAlgorithmException e) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                    Timber.log(Log.ERROR, "SSL handshake failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                }


                if (!UUIDUtil.isValid(request.getRelyingPartyUUID())) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
                    Timber.log(Log.DEBUG, "%s - Relying Party UUID not in valid format", request.getRelyingPartyUUID());
                    return Result.failure();
                }

                try {
                    SessionStatusResponse sessionStatusResponse;
                    String semanticsIdentifier = "PNO" + request.getCountry() + "-" + request.getNationalIdentityNumber();
                    sessionStatusResponse = doSessionStatusRequestLoop(SIDRestServiceClient.getCertificateV2(
                            semanticsIdentifier, getCertificateRequest(request)), true);
                    if (sessionStatusResponse == null) {
                        Timber.log(Log.ERROR, "No session status response");
                        return Result.failure();
                    }

                    Timber.log(Log.DEBUG, "Session status response: %s", sessionStatusResponse.toString());

                    ContainerWrapper containerWrapper = new ContainerWrapper(getApplicationContext(), request.getContainerPath());
                    String base64Hash = containerWrapper.prepareSignature(getCertificatePem(sessionStatusResponse.getCert().getValue()), roleDataRequest);
                    if (base64Hash != null && !base64Hash.isEmpty()) {
                        Timber.log(Log.DEBUG, "Broadcasting signature challenge response");
                        broadcastSmartCreateSignatureChallengeResponse(base64Hash);
                        Thread.sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);

                        String requestString = MessageUtil.toJsonString(getSignatureRequestV2(request, base64Hash));

                        Timber.log(Log.DEBUG, "Request: %s", requestString);

                        sessionStatusResponse = doSessionStatusRequestLoop(SIDRestServiceClient.getCreateSignature(
                                sessionStatusResponse.getResult().getDocumentNumber(), requestString), false);
                        if (sessionStatusResponse == null) {
                            Timber.log(Log.ERROR, "Unable to get session status response");
                            return Result.failure();
                        }
                        Timber.log(Log.DEBUG, "SessionStatusResponse: %s", sessionStatusResponse);
                        Timber.log(Log.DEBUG, "Finalizing signature...");
                        containerWrapper.finalizeSignature(sessionStatusResponse.getSignature().getValue());
                        Timber.log(Log.DEBUG, "Broadcasting signature status response");
                        broadcastSmartCreateSignatureStatusResponse(sessionStatusResponse);
                        return Result.success();
                    } else {
                        Timber.log(Log.DEBUG, "Base64 (Prepare signature) is empty or null");
                        return Result.failure();
                    }
                } catch (UnknownHostException e) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE));
                    Timber.log(Log.ERROR, e, "REST API certificate request failed. Unknown host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                } catch (SSLPeerUnverifiedException e) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                    Timber.log(Log.ERROR, e, "SSL handshake failed - Session status response. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                } catch (SocketTimeoutException ste) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE));
                    Timber.log(Log.ERROR, ste, "Failed to sign with Smart-ID - Unable to connect to service. Exception message: %s. Exception: %s", ste.getMessage(), Arrays.toString(ste.getStackTrace()));
                    return Result.failure();
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("CONNECT: 403")) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID - REST API certificate request failed. Received HTTP status 403. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                        return Result.failure();
                    } else if (e.getMessage() != null && (ProxyUtil.getProxySetting(getApplicationContext()) != NO_PROXY &&
                            e.getMessage().contains("Failed to authenticate with proxy"))) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.INVALID_PROXY_SETTINGS));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID. REST API certificate request failed with current proxy settings. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                        return Result.failure();
                    }
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR, e.getMessage()));
                    Timber.log(Log.ERROR, e, "REST API certificate request failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                } catch (CertificateException e) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR, e.getMessage()));
                    Timber.log(Log.ERROR, "Generating certificate failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                } catch (InterruptedException e) {
                    Timber.log(Log.ERROR, e, "Waiting for next call to SID REST API interrupted. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    Thread.currentThread().interrupt();
                    return Result.failure();
                } catch (NoSuchAlgorithmException e) {
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR, e.getMessage()));
                    Timber.log(Log.ERROR, "Generating verification code failed. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    return Result.failure();
                } catch (Exception e) {
                    Timber.log(Log.ERROR, e, "Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID - Too Many Requests. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    } else if (e.getMessage() != null && e.getMessage().contains("OCSP response not in valid time slot")) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.OCSP_INVALID_TIME_SLOT));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID - OCSP response not in valid time slot. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    } else if (e.getMessage() != null && e.getMessage().contains("Certificate status: revoked")) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.CERTIFICATE_REVOKED));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID - Certificate status: revoked. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    } else if (e.getMessage() != null && e.getMessage().contains("Failed to connect")) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.NO_RESPONSE));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID - Failed to connect to host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    } else if (e.getMessage() != null && e.getMessage().startsWith("Failed to create ssl connection with host")) {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.INVALID_SSL_HANDSHAKE));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID - Failed to create ssl connection with host. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    } else {
                        broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR, e.getMessage()));
                        Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
                    }
                    return Result.failure();
                }
            } else {
                Timber.log(Log.ERROR, "Invalid request");
                throw new IllegalStateException("Invalid request");
            }

    }

    private String getCertificatePem(String cert) {
        return PEM_BEGIN_CERT + "\n" + cert + "\n" + PEM_END_CERT;
    }

    private SessionStatusResponse doSessionStatusRequestLoop(Call<SessionResponse> request, boolean certRequest) throws IOException, SigningCancelledException {
            long timeout = 0;
            SessionResponse sessionResponse = handleRequest(request);
            if (sessionResponse == null) {
                Timber.log(Log.ERROR, "Session response null");
                return null;
            }
            Timber.log(Log.DEBUG, sessionResponse.toString());
            if (sessionResponse.getSessionID() == null || sessionResponse.getSessionID().isEmpty()) {
                broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.MISSING_SESSIONID));
                Timber.log(Log.DEBUG, "Received empty Smart-ID session response");
                return null;
            }

            while (timeout < TIMEOUT_CANCEL) {
                Timber.log(Log.DEBUG, "doSessionStatusRequestLoop timeout counter: %s", timeout);
                SessionStatusResponse sessionStatusResponse = handleRequest(SIDRestServiceClient.getSessionStatus(
                        sessionResponse.getSessionID(), SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS));
                if (sessionStatusResponse == null) {
                    Timber.log(Log.ERROR, "No session status response");
                    return null;
                }
                Timber.log(Log.DEBUG, "doSessionStatusRequestLoop session response: %s", sessionResponse.toString());
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
            broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.TIMEOUT));
            Timber.log(Log.DEBUG, "Request timeout (TIMEOUT)");

        return null;
    }

    private void broadcastFault(ServiceFault serviceFault) {
        Timber.log(Log.DEBUG, "Broadcasting fault: Status: %s, message: %s", serviceFault.getStatus(), serviceFault.getDetailMessage());
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.SERVICE_FAULT)
                .putExtra(SmartSignConstants.SERVICE_FAULT, ServiceFault.toJson(serviceFault));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureStatusResponse(SessionStatusResponse response) {
        Timber.log(Log.DEBUG, "broadcastSmartCreateSignatureStatusResponse: %s", response);
        String smartIdServiceResponse = SmartIDServiceResponse.toJson(generateSmartIdResponse(response));
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(SmartSignConstants.CREATE_SIGNATURE_STATUS,
                        smartIdServiceResponse);
        Timber.log(Log.DEBUG, "Smart-ID service response: " + smartIdServiceResponse);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureSelectDevice() {
        Timber.log(Log.DEBUG, "User selecting device");
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_DEVICE);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private void broadcastSmartCreateSignatureChallengeResponse(String base64Hash) throws NoSuchAlgorithmException {
        Timber.log(Log.DEBUG, "Signature challenge");
        Intent localIntent = new Intent(SmartSignConstants.SID_BROADCAST_ACTION)
                .putExtra(SmartSignConstants.SID_BROADCAST_TYPE_KEY, SmartSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(SmartSignConstants.CREATE_SIGNATURE_CHALLENGE,
                        VerificationCodeUtil.calculateSmartIdVerificationCode(base64Hash));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
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

    private PostCreateSignatureRequestV2 getSignatureRequestV2(
            SmartIDSignatureRequest request, String hash) {
        Timber.log(Log.DEBUG, "Signature request V2: %s", request);
        PostCreateSignatureRequestV2 signatureRequest = new PostCreateSignatureRequestV2();
        signatureRequest.setRelyingPartyUUID(request.getRelyingPartyUUID());
        signatureRequest.setRelyingPartyName(request.getRelyingPartyName());
        signatureRequest.setHashType(request.getHashType());
        signatureRequest.setHash(hash);
        RequestAllowedInteractionsOrder allowedInteractionsOrder = new RequestAllowedInteractionsOrder();
        allowedInteractionsOrder.setType("confirmationMessageAndVerificationCodeChoice");
        allowedInteractionsOrder.setDisplayText200(request.getDisplayText());
        signatureRequest.setAllowedInteractionsOrder(List.of(allowedInteractionsOrder));
        return signatureRequest;
    }

    private <S> S handleRequest(Call<S> request) throws IOException, SigningCancelledException {
        checkSigningCancelled(isCancelled);
        Response<S> httpResponse = request.execute();
        if (!httpResponse.isSuccessful()) {
            Timber.log(Log.DEBUG, "Smart-ID request unsuccessful. Status: %s, message: %s, body: %s, errorBody: %s",
                    httpResponse.code(), httpResponse.message(), httpResponse.body(), httpResponse.errorBody());
            switch (httpResponse.code()) {
                case 401:
                case 403:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.INVALID_ACCESS_RIGHTS));
                    Timber.log(Log.DEBUG, "Forbidden - HTTP status code: %s ", httpResponse.code());
                    break;
                case 404:
                    broadcastFault(new ServiceFault(httpResponse.body() instanceof SessionResponse ?
                            SessionStatusResponse.ProcessStatus.SESSION_NOT_FOUND :
                            SessionStatusResponse.ProcessStatus.ACCOUNT_NOT_FOUND));
                    Timber.log(Log.DEBUG, "Account/session not found - HTTP status code: %s " + httpResponse.code());
                    break;
                case 409:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS));
                    Timber.log(Log.DEBUG, "Exceeded unsuccessful requests - HTTP status code: %s", httpResponse.code());
                    break;
                case 429:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.TOO_MANY_REQUESTS));
                    Timber.log(Log.DEBUG, "Too many requests - HTTP status code: %s", httpResponse.code());
                    break;
                case 471:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.NOT_QUALIFIED));
                    Timber.log(Log.DEBUG, "Not qualified - HTTP status code: %s", httpResponse.code());
                    break;
                case 480:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.OLD_API));
                    Timber.log(Log.DEBUG, "Old API - HTTP status code: %s", httpResponse.code());
                    break;
                case 580:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.UNDER_MAINTENANCE));
                    Timber.log(Log.DEBUG, "Under maintenance - HTTP status code: %s", httpResponse.code());
                    break;
                default:
                    broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.TECHNICAL_ERROR));
                    Timber.log(Log.DEBUG, "Request unsuccessful, technical or general error, HTTP status code: %s " + httpResponse.code());
                    break;
            }
            return null;
        }
        Timber.log(Log.DEBUG, "Response status: %s, response body: %s", httpResponse.code(), httpResponse.body());
        Timber.log(Log.DEBUG, "Smart-ID request: isSuccessful: %s, status: %s, message: %s, body: %s, errorBody: %s",
                httpResponse.isSuccessful(), httpResponse.code(), httpResponse.message(), httpResponse.body(), httpResponse.errorBody());
        return httpResponse.body();
    }

    private SmartIDSignatureRequest getRequestFromData(String signatureRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SmartIDSignatureRequest smartIDSignatureRequest = objectMapper.readValue(signatureRequest, SmartIDSignatureRequest.class);
            Timber.log(Log.DEBUG, "Smart-ID request from data: %s", smartIDSignatureRequest.toString());
            return smartIDSignatureRequest;
        } catch (JsonProcessingException e) {
            broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR));
            Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID. Failed to process signature request JSON. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
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
            broadcastFault(new ServiceFault(SessionStatusResponse.ProcessStatus.GENERAL_ERROR));
            Timber.log(Log.ERROR, e, "Failed to sign with Smart-ID. Failed to process role data request JSON. Exception message: %s. Exception: %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        return null;
    }
}
