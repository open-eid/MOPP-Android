package ee.ria.mopp.androidmobileid.service;


import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

import ee.ria.mopp.androidmobileid.dto.request.GetMobileCreateSignatureStatusRequest;
import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.dto.request.RequestObject;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopp.androidmobileid.dto.response.MobileCreateSignatureResponse;
import ee.ria.mopp.androidmobileid.dto.response.SoapFault;
import ee.ria.mopp.androidmobileid.soap.DigidocServiceClient;
import ee.ria.mopp.androidmobileid.soap.ErrorUtils;
import ee.ria.mopp.androidmobileid.soap.RequestBody;
import ee.ria.mopp.androidmobileid.soap.RequestEnvelope;
import ee.ria.mopp.androidmobileid.soap.ServiceGenerator;
import retrofit2.Call;
import retrofit2.Response;

import static ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest.fromJson;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;

public class MobileSignService extends IntentService {

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 10000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 5000;
    public static final String TAG = MobileSignService.class.getName();
    private DigidocServiceClient ddsClient;

    public MobileSignService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MobileCreateSignatureRequest request = getRequestFromIntent(intent);
        ddsClient = ServiceGenerator.createService(DigidocServiceClient.class);
        Call<MobileCreateSignatureResponse> call = ddsClient.mobileCreateSignature(wrapInEnvelope(request));

        try {
            Response<MobileCreateSignatureResponse> responseWrapper = call.execute();
            if (!responseWrapper.isSuccessful()) {
                SoapFault fault = ErrorUtils.parseError(responseWrapper);
                broadcastFault(fault);
            } else {
                MobileCreateSignatureResponse response = responseWrapper.body();
                broadcastMobileCreateSignatureResponse(response);
                sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
                doCreateSignatureStatusRequestLoop(new GetMobileCreateSignatureStatusRequest(response.getSesscode()));
            }
        } catch (IOException e) {
           Log.e(TAG, "SoapRequestFailure", e);
        }
    }

    private void broadcastFault(SoapFault fault) {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.SERVICE_FAULT)
                .putExtra(MobileSignConstants.SERVICE_FAULT, SoapFault.toJson(fault));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void doCreateSignatureStatusRequestLoop(GetMobileCreateSignatureStatusRequest request) throws IOException {
        Call<GetMobileCreateSignatureStatusResponse> responseCall = ddsClient.getMobileCreateSignatureStatus(wrapInEnvelope(request));
        GetMobileCreateSignatureStatusResponse response = responseCall.execute().body();
        if (response.getStatus() == GetMobileCreateSignatureStatusResponse.ProcessStatus.OUTSTANDING_TRANSACTION) {
            broadcastMobileCreateSignatureStatusResponse(response);
            sleep(SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
            doCreateSignatureStatusRequestLoop(request);
        } else {
            broadcastMobileCreateSignatureStatusResponse(response);
        }
    }

    private RequestEnvelope wrapInEnvelope(RequestObject request) {
        return new RequestEnvelope(new RequestBody(request));
    }

    private void broadcastMobileCreateSignatureStatusResponse(GetMobileCreateSignatureStatusResponse status) {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_STATUS, GetMobileCreateSignatureStatusResponse.toJson(status));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastMobileCreateSignatureResponse(MobileCreateSignatureResponse challenge) {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_CHALLENGE, MobileCreateSignatureResponse.toJson(challenge));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private MobileCreateSignatureRequest getRequestFromIntent(Intent intent) {
        return fromJson(intent.getStringExtra(CREATE_SIGNATURE_REQUEST));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
