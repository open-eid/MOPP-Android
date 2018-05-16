package ee.ria.DigiDoc.android.signature.update;

import android.app.Application;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileAlreadyExistsException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSendIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ContainerLoadAction,
                                        Result.ContainerLoadResult> containerLoad;

    private final ObservableTransformer<Intent.NameUpdateIntent, Result.NameUpdateResult>
            nameUpdate;

    private final ObservableTransformer<Action.DocumentsAddAction,
                                        Result.DocumentsAddResult> documentsAdd;

    private final ObservableTransformer<Action.DocumentOpenAction,
                                        Result.DocumentOpenResult> documentOpen;

    private final ObservableTransformer<Action.DocumentRemoveAction,
                                        Result.DocumentRemoveResult> documentRemove;

    private final ObservableTransformer<Action.SignatureRemoveAction,
                                        Result.SignatureRemoveResult> signatureRemove;

    private final ObservableTransformer<Action.SignatureAddAction,
                                        Result.SignatureAddResult> signatureAdd;

    private final ObservableTransformer<Action.SendAction, Result.SendResult> send;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource,
                      SignatureAddSource signatureAddSource, Application application,
                      Navigator navigator) {
        containerLoad = upstream -> upstream.switchMap(action ->
                signatureContainerDataSource.get(action.containerFile())
                        .toObservable()
                        .switchMap(container -> {
                            if (action.signatureAddSuccessMessageVisible()) {
                                return Observable.timer(3, TimeUnit.SECONDS)
                                        .map(ignored ->
                                                Result.ContainerLoadResult.success(container, null,
                                                        false))
                                        .startWith(Result.ContainerLoadResult.success(container,
                                                null, true));
                            } else {
                                return Observable.just(Result.ContainerLoadResult.success(container,
                                        action.signatureAddMethod(),
                                        action.signatureAddSuccessMessageVisible()));
                            }
                        })
                        .onErrorReturn(Result.ContainerLoadResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.ContainerLoadResult.progress()));

        nameUpdate = upstream -> upstream.switchMap(action -> {
            File containerFile = action.containerFile();
            String name = action.name();

            if (containerFile == null) {
                return Observable.just(Result.NameUpdateResult.hide());
            } else if (name == null) {
                return Observable.just(
                        Result.NameUpdateResult.name(containerFile),
                        Result.NameUpdateResult.show(containerFile));
            } else if (name.equals(containerFile.getName())) {
                return Observable.just(Result.NameUpdateResult.hide());
            } else if (name.isEmpty()) {
                return Observable.just(Result.NameUpdateResult
                        .failure(containerFile, new IOException()));
            } else {
                return Observable
                        .fromCallable(() -> {
                            File newFile = new File(containerFile.getParentFile(), name);
                            if (newFile.createNewFile()) {
                                //noinspection ResultOfMethodCallIgnored
                                newFile.delete();
                                if (!containerFile.renameTo(newFile)) {
                                    throw new IOException();
                                }
                                return newFile;
                            } else {
                                throw new FileAlreadyExistsException(newFile);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(newFile -> {
                            navigator.execute(Transaction.replace(SignatureUpdateScreen
                                    .create(true, false, newFile, false, false)));
                            return Result.NameUpdateResult.progress(newFile);
                        })
                        .onErrorReturn(throwable ->
                                Result.NameUpdateResult.failure(containerFile, throwable))
                        .startWith(Result.NameUpdateResult.progress(containerFile));
            }
        });

        documentsAdd = upstream -> upstream
                .switchMap(action -> {
                    if (action.containerFile() == null) {
                        return Observable.just(Result.DocumentsAddResult.clear());
                    } else {
                        navigator.execute(action.transaction());
                        return navigator.activityResults()
                                .filter(activityResult ->
                                        activityResult.requestCode()
                                                == action.transaction().requestCode())
                                .switchMap(activityResult -> {
                                    if (activityResult.resultCode() == RESULT_OK) {
                                        return signatureContainerDataSource
                                                .addDocuments(action.containerFile(),
                                                        parseGetContentIntent(
                                                                application.getContentResolver(),
                                                                activityResult.data()))
                                                .toObservable()
                                                .map(Result.DocumentsAddResult::success)
                                                .onErrorReturn(Result.DocumentsAddResult::failure)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .startWith(Result.DocumentsAddResult.adding());
                                    } else {
                                        return Observable.just(Result.DocumentsAddResult.clear());
                                    }
                                });
                    }
                });

        documentOpen = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.DocumentOpenResult.clear());
            } else {
                return signatureContainerDataSource
                        .getDocumentFile(action.containerFile(), action.document())
                        .toObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(documentFile -> {
                            if (SignedContainer.isContainer(documentFile)) {
                                navigator.execute(Transaction.push(SignatureUpdateScreen
                                        .create(true, true, documentFile, false, false)));
                                return Result.DocumentOpenResult.clear();
                            } else {
                                return Result.DocumentOpenResult.success(documentFile);
                            }
                        })
                        .onErrorReturn(Result.DocumentOpenResult::failure)
                        .startWith(Result.DocumentOpenResult.opening());
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
                        .toObservable()
                        .map(Result.SignatureRemoveResult::success)
                        .onErrorReturn(Result.SignatureRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.SignatureRemoveResult.progress());
            }
        });

        signatureAdd = upstream -> upstream.switchMap(action -> {
            Integer method = action.method();
            Boolean existingContainer = action.existingContainer();
            File containerFile = action.containerFile();
            SignatureAddRequest request = action.request();
            if (method == null) {
                return Observable.just(Result.SignatureAddResult.clear());
            } else if (request == null && existingContainer != null && containerFile != null) {
                if (SignedContainer.isLegacyContainer(containerFile)) {
                    return signatureContainerDataSource
                            .addContainer(ImmutableList.of(FileStream.create(containerFile)), true)
                            .toObservable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(containerAdd ->
                                    navigator.execute(Transaction.push(SignatureUpdateScreen.create(
                                            containerAdd.isExistingContainer(), false,
                                            containerAdd.containerFile(), true, false))))
                            .map(containerAdd -> Result.SignatureAddResult.clear())
                            .onErrorReturn(Result.SignatureAddResult::failure)
                            .startWith(Result.SignatureAddResult.activity());
                } else {
                    return signatureAddSource.show(method);
                }
            } else if (existingContainer != null && containerFile != null) {
                return signatureAddSource.sign(containerFile, request)
                        .switchMap(response -> {
                            if (response.container() != null) {
                                if (existingContainer) {
                                    return Observable
                                            .timer(3, TimeUnit.SECONDS)
                                            .map(ignored -> Result.SignatureAddResult.clear())
                                            .startWith(Result.SignatureAddResult
                                                    .success(response.container()));
                                } else {
                                    return Observable.fromCallable(() -> {
                                        navigator.execute(Transaction.replace(SignatureUpdateScreen
                                                .create(true, false, containerFile, false, true)));
                                        return Result.SignatureAddResult.method(method, response);
                                    });
                                }
                            } else {
                                return Observable
                                        .just(Result.SignatureAddResult.method(method, response));
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn(Result.SignatureAddResult::failure)
                        .startWith(Result.SignatureAddResult.activity(method));
            } else {
                throw new IllegalArgumentException("Can't handle action " + action);
            }
        });

        send = upstream -> upstream
                .doOnNext(action ->
                        navigator.execute(Transaction.activity(
                                createSendIntent(application, action.containerFile()), null)))
                .map(action -> Result.SendResult.success())
                .onErrorReturn(Result.SendResult::failure);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ContainerLoadAction.class).compose(containerLoad),
                shared.ofType(Intent.NameUpdateIntent.class).compose(nameUpdate),
                shared.ofType(Action.DocumentsAddAction.class).compose(documentsAdd),
                shared.ofType(Action.DocumentOpenAction.class).compose(documentOpen),
                shared.ofType(Action.DocumentRemoveAction.class).compose(documentRemove),
                shared.ofType(Action.SignatureRemoveAction.class).compose(signatureRemove),
                shared.ofType(Action.SignatureAddAction.class).compose(signatureAdd),
                shared.ofType(Action.SendAction.class).compose(send)));
    }
}
