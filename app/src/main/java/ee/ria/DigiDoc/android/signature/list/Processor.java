package ee.ria.DigiDoc.android.signature.list;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.LocaleService;
import ee.ria.DigiDoc.android.utils.files.SignedFilesUtil;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.sign.SignatureStatus;
import ee.ria.DigiDoc.sign.SignedContainer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ContainersLoadAction, Result.ContainersLoadResult>
            containersLoad;

    private final ObservableTransformer<Action.NavigateUpAction, Result.VoidResult> navigateUp;

    private final ObservableTransformer<Action.ContainerOpenAction, Result.VoidResult>
            containerOpen;

    private final ObservableTransformer<Action.ContainerRemoveAction, Result.ContainerRemoveResult>
            containerRemove;

    @Inject Processor(Application application, Navigator navigator,
                      SignatureContainerDataSource signatureContainerDataSource,
                      LocaleService localeService) {
        containersLoad = upstream -> upstream.switchMap(action ->
                signatureContainerDataSource.find()
                        .toObservable()
                        .map((containerFiles) -> Result.ContainersLoadResult.success(navigator.activity(),containerFiles))
                        .onErrorReturn(Result.ContainersLoadResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.ContainersLoadResult
                                .progress(action.indicateActivity())));

        navigateUp = upstream -> upstream
                .doOnNext(action -> navigator.execute(action.transaction()))
                .map(action -> Result.VoidResult.cancel());

        containerOpen = upstream -> upstream.switchMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.VoidResult.cancel());
            } else if (action.confirmation()) {
                return Observable
                        .just(Result.VoidResult.confirmation(action.containerFile()));
            } else {
                File containerFile = action.containerFile();
                if (CryptoContainer.isContainerFileName(containerFile.getName())) {
                    navigator.execute(Transaction.push(CryptoCreateScreen.open(containerFile, false)));
                } else {
                    navigator.execute(Transaction.push(SignatureUpdateScreen
                            .create(true, false, containerFile, false, false,
                                    SignedContainer.isAsicsFile(containerFile.getName()) ?
                                            SignedFilesUtil.getContainerDataFile(signatureContainerDataSource,
                                                    SignedContainer.open(containerFile)) : null, action.isSivaConfirmed())));
                    try {
                        SignedContainer signedContainer = SignedContainer.open(containerFile);
                        sendContainerStatusAccessibilityMessage(signedContainer, application.getApplicationContext(), localeService.applicationConfigurationWithLocale(application.getApplicationContext(),
                                localeService.applicationLocale()));
                    } catch (Exception e) {
                        Timber.log(Log.ERROR, e, String.format("Unable to open container. Error: %s", e.getMessage()));
                        return Observable.just(Result.VoidResult.cancel());
                    }
                }
                return Observable.just(Result.VoidResult.success());
            }
        });

        containerRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.ContainerRemoveResult.cancel());
            } else if (action.confirmation()) {
                return Observable
                        .just(Result.ContainerRemoveResult.confirmation(action.containerFile()));
            } else {
                return signatureContainerDataSource.remove(action.containerFile())
                        .andThen(signatureContainerDataSource.find())
                        .toObservable()
                        .map(file -> {
                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.document_removed);
                            return Result.ContainerRemoveResult.success(navigator.activity(), file);
                        })
                        .onErrorReturn(Result.ContainerRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.ContainerRemoveResult.progress());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ContainersLoadAction.class).compose(containersLoad),
                shared.ofType(Action.NavigateUpAction.class).compose(navigateUp),
                shared.ofType(Action.ContainerRemoveAction.class).compose(containerRemove),
                shared.ofType(Action.ContainerOpenAction.class).compose(containerOpen)));
    }

    private void sendContainerStatusAccessibilityMessage(SignedContainer container, Context context, Configuration configuration) {
        Context configurationContext = context.createConfigurationContext(configuration);
        StringBuilder messageBuilder = new StringBuilder();
        if (container.signaturesValid()) {
            int validSignaturesCount = container.signatures().size();
            messageBuilder.append(configurationContext.getResources().getString(R.string.container_has));
            messageBuilder.append(" ");
            messageBuilder.append(configurationContext.getResources().getQuantityString(
                    R.plurals.signature_update_signatures_valid, validSignaturesCount, validSignaturesCount));
        } else {
            int unknownSignaturesCount = container.invalidSignatureCounts().get(SignatureStatus.UNKNOWN);
            int invalidSignatureCount = container.invalidSignatureCounts().get(SignatureStatus.INVALID);
            messageBuilder.append("Container is invalid, contains");
            if (unknownSignaturesCount > 0) {
                messageBuilder.append(" ").append(configurationContext.getResources().getQuantityString(
                        R.plurals.signature_update_signatures_unknown, unknownSignaturesCount, unknownSignaturesCount).toLowerCase());
            }
            if (invalidSignatureCount > 0) {
                messageBuilder.append(" ").append(configurationContext.getResources().getQuantityString(
                        R.plurals.signature_update_signatures_invalid, invalidSignatureCount, invalidSignatureCount));
            }
        }
        AccessibilityUtils.sendAccessibilityEvent(configurationContext, AccessibilityEvent.TYPE_ANNOUNCEMENT, messageBuilder.toString());
    }
}
