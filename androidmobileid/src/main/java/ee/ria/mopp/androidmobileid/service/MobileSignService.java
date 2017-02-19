package ee.ria.mopp.androidmobileid.service;


import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import ee.ria.mopp.androidmobileid.dto.request.GetMobileCreateSignatureStatusRequest;
import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopp.androidmobileid.dto.response.MobileCreateSignatureResponse;
import ee.ria.mopp.androidmobileid.dto.response.ServiceFault;
import ee.ria.mopp.androidmobileid.soap.DigidocServiceClient;
import ee.ria.mopp.androidmobileid.soap.ErrorUtils;
import ee.ria.mopp.androidmobileid.soap.RequestBody;
import ee.ria.mopp.androidmobileid.soap.RequestEnvelope;
import ee.ria.mopp.androidmobileid.soap.RequestObject;
import ee.ria.mopp.androidmobileid.soap.ServiceGenerator;
import ee.ria.mopp.androidmobileid.soap.SoapFault;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest.fromJson;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;

public class MobileSignService extends IntentService {

    public static final String TAG = MobileSignService.class.getName();

    private static final long INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 10000;
    private static final long SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS = 5000;

    private DigidocServiceClient ddsClient;

    public MobileSignService() {
        super(TAG);
        Timber.tag(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("Handling mobile sign intent");
        MobileCreateSignatureRequest request = getRequestFromIntent(intent);
        try {
            SSLContext ddsSSLConfig = createSSLConfig(intent);
            ddsClient = ServiceGenerator.createService(DigidocServiceClient.class, ddsSSLConfig);
        } catch (Exception e) {
            ddsClient = ServiceGenerator.createService(DigidocServiceClient.class, null);
        }

        Call<MobileCreateSignatureResponse> call = ddsClient.mobileCreateSignature(wrapInEnvelope(request));

        try {
            Response<MobileCreateSignatureResponse> responseWrapper = call.execute();
            if (!responseWrapper.isSuccessful()) {
                parseErrorAndBroadcast(responseWrapper);
            } else {
                MobileCreateSignatureResponse response = responseWrapper.body();
                broadcastMobileCreateSignatureResponse(response);
                sleep(INITIAL_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
                doCreateSignatureStatusRequestLoop(new GetMobileCreateSignatureStatusRequest(response.getSesscode()));
            }
        } catch (IOException e) {
            broadcastFault(new ServiceFault(e));
            Timber.e(e, "Soap request to DigiDocService failed");
        }
    }

    private static SSLContext createSSLConfig(Intent intent) throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        String keystorePath = intent.getStringExtra(ACCESS_TOKEN_PATH);
        String keystorePass = intent.getStringExtra(ACCESS_TOKEN_PASS);

        try (InputStream key = new FileInputStream(new File(keystorePath))) {
            String keyStoreType = "PKCS12";
            KeyStore keyStore   = KeyStore.getInstance(keyStoreType);
            keyStore.load(key, keystorePass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, null);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        }
    }

    private void doCreateSignatureStatusRequestLoop(GetMobileCreateSignatureStatusRequest request) throws IOException {
        Call<GetMobileCreateSignatureStatusResponse> responseCall = ddsClient.getMobileCreateSignatureStatus(wrapInEnvelope(request));
        Response<GetMobileCreateSignatureStatusResponse> responseWrapper = responseCall.execute();
        if (!responseWrapper.isSuccessful()) {
            parseErrorAndBroadcast(responseWrapper);
            return;
        }
        GetMobileCreateSignatureStatusResponse response = responseWrapper.body();
        broadcastMobileCreateSignatureStatusResponse(response);
        if (isOutstandingTransaction(response)) {
            sleep(SUBSEQUENT_STATUS_REQUEST_DELAY_IN_MILLISECONDS);
            doCreateSignatureStatusRequestLoop(request);
        }
    }

    private boolean isOutstandingTransaction(GetMobileCreateSignatureStatusResponse response) {
        return response.getStatus() == GetMobileCreateSignatureStatusResponse.ProcessStatus.OUTSTANDING_TRANSACTION;
    }

    private void parseErrorAndBroadcast(Response responseWrapper) {
        SoapFault soapFault = ErrorUtils.parseError(responseWrapper);
        ServiceFault serviceFault = new ServiceFault(soapFault);
        Timber.d("Service fault occured: %s", serviceFault.toString());
        broadcastFault(serviceFault);
    }

    private RequestEnvelope wrapInEnvelope(RequestObject request) {
        return new RequestEnvelope(new RequestBody(request));
    }

    private void broadcastFault(ServiceFault fault) {
        Intent localIntent = new Intent(MobileSignConstants.MID_BROADCAST_ACTION)
                .putExtra(MobileSignConstants.MID_BROADCAST_TYPE_KEY, MobileSignConstants.SERVICE_FAULT)
                .putExtra(MobileSignConstants.SERVICE_FAULT, ServiceFault.toJson(fault));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
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
            Timber.e(e, "Waiting for next call to DigiDocService interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
