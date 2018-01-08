package ee.ria.DigiDoc.android.signature.update;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.navigation.Transaction;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.mid.CreateSignatureRequestBuilder;
import ee.ria.libdigidocpp.Conf;
import ee.ria.libdigidocpp.Container;
import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse.ProcessStatus;
import ee.ria.mopp.androidmobileid.dto.response.MobileCreateSignatureResponse;
import ee.ria.mopp.androidmobileid.dto.response.ServiceFault;
import ee.ria.mopp.androidmobileid.service.MobileSignService;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.io.Files.getFileExtension;
import static ee.ria.DigiDoc.util.FileUtils.getSchemaCacheDirectory;
import static ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest.toJson;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.MID_BROADCAST_ACTION;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.MID_BROADCAST_TYPE_KEY;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.SERVICE_FAULT;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadContainerAction,
                                        Result.LoadContainerResult> loadContainer;

    private final ObservableTransformer<Action.AddDocumentsAction,
                                        Result.AddDocumentsResult> addDocuments;

    private final ObservableTransformer<Action.OpenDocumentAction,
                                        Result.OpenDocumentResult> openDocument;

    private final ObservableTransformer<Action.DocumentRemoveAction,
                                        Result.DocumentRemoveResult> documentRemove;

    private final ObservableTransformer<Action.SignatureRemoveAction,
                                        Result.SignatureRemoveResult> signatureRemove;

    private final ObservableTransformer<Action.SignatureAddAction,
                                        Result.SignatureAddResult> signatureAdd;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource,
                      SettingsDataStore settingsDataStore, Application application) {
        loadContainer = upstream -> upstream.flatMap(action ->
                signatureContainerDataSource.get(action.containerFile())
                        .toObservable()
                        .map(Result.LoadContainerResult::success)
                        .onErrorReturn(AutoValue_Result_LoadContainerResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.LoadContainerResult.progress()));

        addDocuments = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.AddDocumentsResult.clear());
            } else if (action.fileStreams() == null) {
                return Observable.just(Result.AddDocumentsResult.picking());
            } else {
                return signatureContainerDataSource
                        .addDocuments(action.containerFile(), action.fileStreams())
                        .andThen(signatureContainerDataSource.get(action.containerFile()))
                        .toObservable()
                        .map(Result.AddDocumentsResult::success)
                        .onErrorReturn(Result.AddDocumentsResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.AddDocumentsResult.adding());
            }
        });

        openDocument = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.OpenDocumentResult.clear());
            } else {
                return signatureContainerDataSource
                        .getDocumentFile(action.containerFile(), action.document())
                        .toObservable()
                        .map(Result.OpenDocumentResult::success)
                        .onErrorReturn(Result.OpenDocumentResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.OpenDocumentResult.opening());
            }
        });

        documentRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null || action.document() == null) {
                return Observable.just(Result.DocumentRemoveResult.clear());
            } else if (action.showConfirmation()) {
                return Observable.just(Result.DocumentRemoveResult.confirmation(action.document()));
            } else {
                return signatureContainerDataSource
                        .removeDocument(action.containerFile(), action.document())
                        .andThen(signatureContainerDataSource.get(action.containerFile()))
                        .toObservable()
                        .map(Result.DocumentRemoveResult::success)
                        .onErrorReturn(Result.DocumentRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.DocumentRemoveResult.progress());
            }
        });

        signatureRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null || action.signature() == null) {
                return Observable.just(Result.SignatureRemoveResult.clear());
            } else if (action.showConfirmation()) {
                return Observable.just(Result.SignatureRemoveResult
                        .confirmation(action.signature()));
            } else {
                return signatureContainerDataSource
                        .removeSignature(action.containerFile(), action.signature())
                        .andThen(signatureContainerDataSource.get(action.containerFile()))
                        .toObservable()
                        .map(Result.SignatureRemoveResult::success)
                        .onErrorReturn(Result.SignatureRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.SignatureRemoveResult.progress());
            }
        });

        signatureAdd = upstream -> upstream.flatMap(action -> {
            File containerFile = action.containerFile();
            if (containerFile == null) {
                return Observable.just(Result.SignatureAddResult.clear());
            } else if (action.show()) {
                if (settingsDataStore.getFileTypes()
                        .contains(getFileExtension(containerFile.getName()))) {
                    return Observable.just(Result.SignatureAddResult.show());
                } else {
                    return signatureContainerDataSource
                            .addContainer(ImmutableList.of(FileStream.create(containerFile)), true)
                            .toObservable()
                            .map(newContainerFile -> Result.SignatureAddResult.transaction(
                                    Transaction.PushScreenTransaction.create(
                                            SignatureUpdateScreen.create(newContainerFile))))
                            .onErrorReturn(Result.SignatureAddResult::failure)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(Result.SignatureAddResult.creatingContainer());
                }
            } else {
                if (firstNonNull(action.rememberMe(), false)) {
                    settingsDataStore.setPhoneNo(action.phoneNo());
                    settingsDataStore.setPersonalCode(action.personalCode());
                }
                return Observable
                        .create(new MobileIdOnSubscribe(application, containerFile,
                                action.phoneNo(), action.personalCode(),
                                settingsDataStore.getSignatureProfile()))
                        .onErrorReturn(Result.SignatureAddResult::failure)
                        .flatMap(result -> {
                            if (result.signature() != null) {
                                return signatureContainerDataSource
                                        .addSignature(containerFile, result.signature())
                                        .andThen(signatureContainerDataSource.get(containerFile))
                                        .map(Result.SignatureAddResult::success)
                                        .toObservable()
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread());
                            } else {
                                return Observable.just(result);
                            }
                        })
                        .startWith(Result.SignatureAddResult.status(null));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.LoadContainerAction.class).compose(loadContainer),
                shared.ofType(Action.AddDocumentsAction.class).compose(addDocuments),
                shared.ofType(Action.OpenDocumentAction.class).compose(openDocument),
                shared.ofType(Action.DocumentRemoveAction.class).compose(documentRemove),
                shared.ofType(Action.SignatureRemoveAction.class).compose(signatureRemove),
                shared.ofType(Action.SignatureAddAction.class).compose(signatureAdd)));
    }

    static final class MobileIdOnSubscribe implements
            ObservableOnSubscribe<Result.SignatureAddResult> {

        private final Application application;
        private final LocalBroadcastManager broadcastManager;
        private final File containerFile;
        private final String phoneNo;
        private final String personalCode;
        private final String signatureProfile;

        MobileIdOnSubscribe(Application application, File containerFile, String phoneNo,
                            String personalCode, String signatureProfile) {
            this.application = application;
            this.broadcastManager = LocalBroadcastManager.getInstance(application);
            this.containerFile = containerFile;
            this.phoneNo = phoneNo;
            this.personalCode = personalCode;
            this.signatureProfile = signatureProfile;
        }

        @Override
        public void subscribe(ObservableEmitter<Result.SignatureAddResult> e) throws Exception {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                e.onError(new IOException("Could not open signature container " + containerFile));
                return;
            }
            Conf conf = Conf.instance();
            ContainerFacade containerFacade = new ContainerFacade(container, containerFile);
            if (containerFacade.isSignedBy(personalCode)) {
                e.onError(new SignatureAlreadyExistsException());
                return;
            }

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getStringExtra(MID_BROADCAST_TYPE_KEY)) {
                        case SERVICE_FAULT:
                            ServiceFault fault = ServiceFault
                                    .fromJson(intent.getStringExtra(SERVICE_FAULT));
                            e.onError(new MobileIdFaultReasonMessageException(fault.getReason()));
                            break;
                        case CREATE_SIGNATURE_CHALLENGE:
                            MobileCreateSignatureResponse challenge = MobileCreateSignatureResponse
                                    .fromJson(intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE));
                            e.onNext(Result.SignatureAddResult.challenge(
                                    challenge.getChallengeID()));
                            break;
                        case CREATE_SIGNATURE_STATUS:
                            GetMobileCreateSignatureStatusResponse status =
                                    GetMobileCreateSignatureStatusResponse.fromJson(
                                            intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                            switch (status.getStatus()) {
                                case OUTSTANDING_TRANSACTION:
                                    e.onNext(Result.SignatureAddResult.status(status.getStatus()));
                                    break;
                                case SIGNATURE:
                                    e.onNext(Result.SignatureAddResult.signature(
                                            status.getSignature()));
                                    break;
                                default:
                                    e.onError(new MobileIdMessageException(status.getStatus()));
                                    break;
                            }
                            break;
                    }
                }
            };

            broadcastManager.registerReceiver(receiver, new IntentFilter(MID_BROADCAST_ACTION));
            e.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

            String message = application.getString(R.string.action_sign) + " " +
                    containerFacade.getName();
            MobileCreateSignatureRequest request = CreateSignatureRequestBuilder
                    .aCreateSignatureRequest()
                    .withContainer(containerFacade)
                    .withIdCode(personalCode)
                    .withPhoneNr(phoneNo)
                    .withDesiredMessageToDisplay(message)
                    .withLocale(Locale.getDefault())
                    .withLocalSigningProfile(signatureProfile)
                    .build();
            android.content.Intent intent = new Intent(application, MobileSignService.class);
            intent.putExtra(CREATE_SIGNATURE_REQUEST, toJson(request));
            intent.putExtra(ACCESS_TOKEN_PASS, conf == null ? "" : conf.PKCS12Pass());
            intent.putExtra(ACCESS_TOKEN_PATH,
                    new File(getSchemaCacheDirectory(application), "878252.p12").getAbsolutePath());
            application.startService(intent);
        }
    }

    static final class SignatureAlreadyExistsException extends Exception {

        SignatureAlreadyExistsException() {
        }

        @Override
        public String toString() {
            return "SignatureAlreadyExistsException{}";
        }
    }

    static final class MobileIdMessageException extends Exception {

        final ProcessStatus processStatus;

        MobileIdMessageException(ProcessStatus processStatus) {
            this.processStatus = processStatus;
        }

        @Override
        public String toString() {
            return "MobileIdMessageException{" +
                    "processStatus=" + processStatus +
                    '}';
        }
    }

    static final class MobileIdFaultReasonMessageException extends Exception {

        final String reason;

        MobileIdFaultReasonMessageException(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "MobileIdFaultReasonMessageException{" +
                    "reason='" + reason + '\'' +
                    '}';
        }
    }
}
