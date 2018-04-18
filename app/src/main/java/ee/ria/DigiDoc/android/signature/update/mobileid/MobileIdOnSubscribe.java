package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.Locale;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdMessageException;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.mid.CreateSignatureRequestBuilder;
import ee.ria.DigiDoc.mid.MobileSignFaultMessageSource;
import ee.ria.DigiDoc.mid.MobileSignStatusMessageSource;
import ee.ria.libdigidocpp.Conf;
import ee.ria.libdigidocpp.Container;
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
    private final MobileSignStatusMessageSource statusMessageSource;
    private final MobileSignFaultMessageSource faultMessageSource;

    public MobileIdOnSubscribe(Application application, SignedContainer container,
                               String personalCode, String phoneNo) {
        this.application = application;
        this.container = container;
        this.broadcastManager = LocalBroadcastManager.getInstance(application);
        this.personalCode = personalCode;
        this.phoneNo = phoneNo;
        this.statusMessageSource = new MobileSignStatusMessageSource(application.getResources());
        this.faultMessageSource = new MobileSignFaultMessageSource(application.getResources());
    }

    @Override
    public void subscribe(ObservableEmitter<MobileIdResponse> emitter) throws Exception {
        Container container = Container.open(this.container.file().getAbsolutePath());
        if (container == null) {
            emitter.onError(new IOException("Could not open signature container " +
                    this.container.file()));
            return;
        }
        Conf conf = Conf.instance();
        ContainerFacade containerFacade = new ContainerFacade(container, this.container.file());

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(MID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT:
                        ServiceFault fault = ServiceFault
                                .fromJson(intent.getStringExtra(SERVICE_FAULT));
                        emitter.onError(new MobileIdMessageException(
                                faultMessageSource.getMessage(fault.getReason())));
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
                                emitter.onError(new MobileIdMessageException(
                                        statusMessageSource.getMessage(status.getStatus())));
                                break;
                        }
                        break;
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(MID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        String message = application.getString(R.string.action_sign) + " " +
                containerFacade.getName();
        MobileCreateSignatureRequest request = CreateSignatureRequestBuilder
                .aCreateSignatureRequest()
                .withContainer(containerFacade)
                .withIdCode(personalCode)
                .withPhoneNr(phoneNo)
                .withDesiredMessageToDisplay(message)
                .withLocale(Locale.getDefault())
                .withLocalSigningProfile(this.container.signatureProfile())
                .build();
        android.content.Intent intent = new Intent(application, MobileSignService.class);
        intent.putExtra(CREATE_SIGNATURE_REQUEST, toJson(request));
        intent.putExtra(ACCESS_TOKEN_PASS, conf == null ? "" : conf.PKCS12Pass());
        intent.putExtra(ACCESS_TOKEN_PATH,
                MoppLib.accessCertificateDir(application).getAbsolutePath());
        application.startService(intent);
    }
}
