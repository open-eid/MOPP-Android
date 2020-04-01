package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdMessageException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.DigiDoc.mobileid.dto.response.RESTServiceFault;
import ee.ria.DigiDoc.mobileid.service.MobileSignService;
import ee.ria.DigiDoc.sign.SignLib;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import static ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest.toJson;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MID_BROADCAST_ACTION;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MID_BROADCAST_TYPE_KEY;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SERVICE_FAULT;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SIGN_SERVICE_URL;

public final class MobileIdOnSubscribe implements ObservableOnSubscribe<MobileIdResponse> {

    private final Navigator navigator;
    private final SignedContainer container;
    private final LocalBroadcastManager broadcastManager;
    private final String personalCode;
    private final String phoneNo;

    public MobileIdOnSubscribe(Navigator navigator, SignedContainer container, String personalCode,
                               String phoneNo) {
        this.navigator = navigator;
        this.container = container;
        this.broadcastManager = LocalBroadcastManager.getInstance(navigator.activity());
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
                        RESTServiceFault fault = RESTServiceFault
                                .fromJson(intent.getStringExtra(SERVICE_FAULT));
                        if (fault.getStatus() != null) {
                            emitter.onError(MobileIdMessageException
                                    .create(navigator.activity(), fault.getStatus()));
                        } else {
                            emitter.onError(MobileIdMessageException
                                    .create(navigator.activity(), fault.getResult()));
                        }
                        break;
                    case CREATE_SIGNATURE_CHALLENGE:
                        String challenge =
                                intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE);
                        emitter.onNext(MobileIdResponse.challenge(challenge));
                        break;
                    case CREATE_SIGNATURE_STATUS:
                        ee.ria.DigiDoc.mobileid.dto.response.MobileIdResponse status =
                                ee.ria.DigiDoc.mobileid.dto.response.MobileIdResponse.fromJson(
                                        intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                        switch (status.getStatus()) {
                            case USER_CANCELLED:
                                emitter.onNext(MobileIdResponse.status(status.getStatus()));
                                break;
                            case OK:
                                emitter.onNext(MobileIdResponse.signature(status.getSignature()));
                                emitter.onNext(MobileIdResponse.success(container));
                                emitter.onComplete();
                                break;
                            default:
                                emitter.onError(MobileIdMessageException
                                        .create(navigator.activity(), status.getStatus()));
                                break;
                        }
                        break;
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(MID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        String displayMessage = navigator.activity()
                .getString(R.string.signature_update_mobile_id_display_message);
        MobileCreateSignatureRequest request = MobileCreateSignatureRequestHelper
                .create(container, personalCode, phoneNo, displayMessage);

        android.content.Intent intent = new Intent(navigator.activity(), MobileSignService.class);
        intent.putExtra(CREATE_SIGNATURE_REQUEST, toJson(request));
        intent.putExtra(ACCESS_TOKEN_PASS, SignLib.accessTokenPass());
        intent.putExtra(ACCESS_TOKEN_PATH, SignLib.accessTokenPath());
        intent.putExtra(SIGN_SERVICE_URL, ((Application) navigator.activity().getApplication()).getConfigurationProvider().getMidRestUrl() + "/");
        navigator.activity().startService(intent);
    }
}
