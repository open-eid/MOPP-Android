package ee.ria.mopp.androidmobileid.service;


import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import ee.ria.mopp.androidmobileid.dto.ChallengeDto;
import ee.ria.mopp.androidmobileid.dto.CreateSignatureStatusDto;
import ee.ria.mopp.androidmobileid.dto.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.soap.DigiDocService;

import static ee.ria.mopp.androidmobileid.dto.MobileCreateSignatureRequest.fromJson;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;

public class MobileSignService extends IntentService {

    public static final String TAG = MobileSignService.class.getName();

    private DigiDocService dds;

    public MobileSignService() {
        super(TAG);
        dds = new DigiDocService();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MobileCreateSignatureRequest request = getRequestFromIntent(intent);
        ChallengeDto challenge = dds.mobileCreateSignature(request);


        Log.i(TAG, "Created object form json: " + request.toString());
        sleep(1000);
        broadcastChallenge(createChallenge());
        /*
        sleep(5000);
        broadcastStatus(createPendingStatus());
        sleep(5000);
        broadcastStatus(createPendingStatus());
        sleep(5000);
        broadcastStatus(createSignatureStatus());
        */
    }

    private void broadcastStatus(CreateSignatureStatusDto status) {
        Intent localIntent = new Intent(MobileSignConstants.BROADCAST_ACTION)
                .putExtra(MobileSignConstants.BROADCAST_TYPE, MobileSignConstants.CREATE_SIGNATURE_STATUS)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_STATUS, CreateSignatureStatusDto.toJson(status));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void broadcastChallenge(ChallengeDto challenge) {
        Intent localIntent = new Intent(MobileSignConstants.BROADCAST_ACTION)
                .putExtra(MobileSignConstants.BROADCAST_TYPE, MobileSignConstants.CREATE_SIGNATURE_CHALLENGE)
                .putExtra(MobileSignConstants.CREATE_SIGNATURE_CHALLENGE, ChallengeDto.toJson(challenge));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private ChallengeDto createChallenge() {
        ChallengeDto challenge = new ChallengeDto();
        challenge.setChallengeID("1234");
        challenge.setSesscode("sesscodensloögnasögn");
        challenge.setStatus("OK");
        return challenge;
    }

    private CreateSignatureStatusDto createPendingStatus() {
        CreateSignatureStatusDto status = new CreateSignatureStatusDto();
        status.setSessCode("sesscodensloögnasögn");
        status.setStatus(CreateSignatureStatusDto.ProcessStatus.OUTSTANDING_TRANSACTION);
        return status;
    }

    private CreateSignatureStatusDto createSignatureStatus() {
        CreateSignatureStatusDto status = new CreateSignatureStatusDto();
        status.setSessCode("sesscodensloögnasögn");
        status.setStatus(CreateSignatureStatusDto.ProcessStatus.SIGNATURE);
        status.setSignature("KDFGASNASHADLSÖFHMADFHLMADMP)=(GYJ€%(QJMG%Q#J%GQ%Y");
        return status;
    }

    private MobileCreateSignatureRequest getRequestFromIntent(Intent intent) {
        return fromJson(intent.getStringExtra(CREATE_SIGNATURE_REQUEST));
    }

    private void sleep(long millis) {
        Log.i(TAG, "sleeping: " + millis);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        Log.i(TAG, "finished sleeping");
    }
}
