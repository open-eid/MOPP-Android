package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdMessageException;
import ee.ria.libdigidocpp.Conf;
import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopp.androidmobileid.dto.response.MobileCreateSignatureResponse;
import ee.ria.mopp.androidmobileid.dto.response.ServiceFault;
import ee.ria.mopp.androidmobileid.service.MobileSignService;
import ee.ria.mopplib.MoppLib;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import static ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest.toJson;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.MID_BROADCAST_ACTION;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.MID_BROADCAST_TYPE_KEY;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.SERVICE_FAULT;

public final class MobileIdOnSubscribe implements ObservableOnSubscribe<MobileIdResponse> {

    private final Application application;
    private final SignedContainer container;
    private final LocalBroadcastManager broadcastManager;
    private final String personalCode;
    private final String phoneNo;

    public MobileIdOnSubscribe(Application application, SignedContainer container,
                               String personalCode, String phoneNo) {
        this.application = application;
        this.container = container;
        this.broadcastManager = LocalBroadcastManager.getInstance(application);
        this.personalCode = personalCode;
        this.phoneNo = phoneNo;
    }

    @Override
    public void subscribe(ObservableEmitter<MobileIdResponse> emitter) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(MID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT:
                        ServiceFault fault = ServiceFault
                                .fromJson(intent.getStringExtra(SERVICE_FAULT));
                        emitter.onError(MobileIdMessageException
                                .create(application, fault.getReason()));
                        break;
                    case CREATE_SIGNATURE_CHALLENGE:
                        MobileCreateSignatureResponse challenge = MobileCreateSignatureResponse
                                .fromJson(intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE));
                        emitter.onNext(MobileIdResponse.challenge(challenge.getChallengeID()));
                        break;
                    case CREATE_SIGNATURE_STATUS:
                        GetMobileCreateSignatureStatusResponse status =
                                GetMobileCreateSignatureStatusResponse.fromJson(
                                        intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                        switch (status.getStatus()) {
                            case OUTSTANDING_TRANSACTION:
                                emitter.onNext(MobileIdResponse.status(status.getStatus()));
                                break;
                            case SIGNATURE:
                                emitter.onNext(MobileIdResponse.signature(status.getSignature()));
                                emitter.onComplete();
                                break;
                            default:
                                emitter.onError(MobileIdMessageException
                                        .create(application, status.getStatus()));
                                break;
                        }
                        break;
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(MID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        Conf conf = Conf.instance();
        String displayMessage = application
                .getString(R.string.signature_update_mobile_id_display_message, container.name());
        MobileCreateSignatureRequest request = MobileCreateSignatureRequestHelper
                .create(container, personalCode, phoneNo, displayMessage);

        android.content.Intent intent = new Intent(application, MobileSignService.class);
        intent.putExtra(CREATE_SIGNATURE_REQUEST, toJson(request));
        intent.putExtra(ACCESS_TOKEN_PASS, conf == null ? "" : conf.PKCS12Pass());
        intent.putExtra(ACCESS_TOKEN_PATH,
                MoppLib.accessCertificateDir(application).getAbsolutePath());
        application.startService(intent);
    }
}
