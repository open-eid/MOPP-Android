package ee.ria.DigiDoc.android.signature.create;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

import android.app.Application;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.ClickableDialogUtil;
import ee.ria.DigiDoc.android.utils.SivaUtil;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.files.SignedFilesUtil;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResultException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.common.ActivityUtil;
import ee.ria.DigiDoc.sign.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ChooseFilesAction, Result.ChooseFilesResult>
            chooseFiles;

    private final ConfirmationDialog sivaConfirmationDialog;

    private static final ImmutableSet<String> TIMESTAMP_CONTAINER_EXTENSIONS = ImmutableSet.of("asics", "scs");

    @Inject Processor(Navigator navigator,
                      SignatureContainerDataSource signatureContainerDataSource,
                      Application application,
                      FileSystem fileSystem) {
        sivaConfirmationDialog = new ConfirmationDialog(navigator.activity(),
                R.string.siva_send_message_dialog, R.id.sivaConfirmationDialog);

        chooseFiles = upstream -> upstream
                .switchMap(action -> {
                    if (action.intent() != null) {
                        throw new ActivityResultException(ActivityResult.create(
                                action.transaction().requestCode(), RESULT_OK, action.intent()));
                    }
                    navigator.execute(action.transaction());
                    return navigator.activityResults()
                            .filter(activityResult ->
                                    activityResult.requestCode()
                                            == action.transaction().requestCode())
                            .doOnNext(activityResult -> {
                                throw new ActivityResultException(activityResult);
                            })
                            .map(activityResult -> Result.ChooseFilesResult.create());
                })
                .onErrorResumeNext(throwable -> {
                    if (!(throwable instanceof ActivityResultException)) {
                        return Observable.error(throwable);
                    }
                    ActivityResult activityResult = ((ActivityResultException) throwable)
                            .activityResult;
                    if (activityResult.resultCode() == RESULT_OK) {
                        if (activityResult.data() != null) {
                            ImmutableList<FileStream> validFiles = FileSystem.getFilesWithValidSize(
                                    parseGetContentIntent(navigator.activity(), application.getContentResolver(), activityResult.data(), fileSystem.getExternallyOpenedFilesDir()));
                            ToastUtil.handleEmptyFileError(validFiles, application, navigator.activity());

                            return handleFiles(navigator, signatureContainerDataSource, validFiles)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnError(throwable1 -> {
                                        Timber.log(Log.ERROR, throwable1,
                                                String.format("Unable to add file to container. Error: %s",
                                                        throwable1.getLocalizedMessage()));
                                        ToastUtil.showError(navigator.activity(), R.string.signature_create_error);

                                        navigator.execute(Transaction.pop());
                                    });
                        } else {
                            Timber.log(Log.ERROR, "Data from file chooser is empty");
                            ToastUtil.showError(navigator.activity(), R.string.signature_create_error);

                            navigator.execute(Transaction.pop());
                        }
                    } else {
                        if (ActivityUtil.isExternalFileOpened(navigator.activity())) {
                            ActivityUtil.restartActivity(application.getApplicationContext(), navigator.activity());
                        } else {
                            navigator.execute(Transaction.pop());
                        }
                        return Observable.just(Result.ChooseFilesResult.create());
                    }

                    return Observable.empty();
                })
                .onErrorReturn(throwable -> {
                    List<Throwable> exceptions = ((CompositeException) throwable).getExceptions();
                    if (!exceptions.isEmpty()) {
                        boolean isEmptyFileException = exceptions.stream().anyMatch(exception ->
                                (exception instanceof EmptyFileException));
                        if (isEmptyFileException) {
                            ToastUtil.showEmptyFileError(navigator.activity(), application);
                        } else {
                            ToastUtil.showError(navigator.activity(), R.string.signature_create_error);
                        }
                    }
                    navigator.execute(Transaction.pop());
                    return Result.ChooseFilesResult.create();
                });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ChooseFilesAction.class).compose(chooseFiles)));
    }

    private Observable<Result.ChooseFilesResult> addFilesToContainer(Navigator navigator,
                                                                     SignatureContainerDataSource signatureContainerDataSource,
                                                                     ImmutableList<FileStream> validFiles) {
        return signatureContainerDataSource
                .addContainer(navigator.activity(), validFiles, false)
                .toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(containerAdd ->
                        navigator.execute(Transaction.replace(SignatureUpdateScreen
                                .create(containerAdd.isExistingContainer(), false,
                                        containerAdd.containerFile(), false,
                                        false,
                                        SignedContainer.isAsicsFile(containerAdd.containerFile().getName()) ?
                                                SignedFilesUtil.getContainerDataFile(signatureContainerDataSource,
                                                        SignedContainer.open(containerAdd.containerFile())) : null))))
                .doOnError(throwable1 -> {
                    Timber.log(Log.ERROR, throwable1, "Add signed container failed");
                    if (throwable1 instanceof NoInternetConnectionException) {
                        ToastUtil.showError(navigator.activity(), R.string.no_internet_connection);
                    } else {
                        ToastUtil.showError(navigator.activity(), R.string.signature_create_error);
                    }

                    navigator.execute(Transaction.pop());
                })
                .map(containerAdd -> Result.ChooseFilesResult.create());
    }

    private Observable<Result.ChooseFilesResult> handleFiles(Navigator navigator,
                                                             SignatureContainerDataSource signatureContainerDataSource,
                                                             ImmutableList<FileStream> validFiles) {
        return SivaUtil.isSivaConfirmationNeeded(validFiles, navigator.activity())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(isSivaConfirmationNeeded -> {
                    if (isSivaConfirmationNeeded) {
                        sivaConfirmationDialog.show();
                        ClickableDialogUtil.makeLinksInDialogClickable(sivaConfirmationDialog);
                        sivaConfirmationDialog.cancels()
                                .flatMap(next -> {
                                    if (validFiles.size() == 1 && SignedContainer.isAsicsFile(validFiles.get(0).displayName().toLowerCase())) {
                                        sivaConfirmationDialog.dismiss();
                                        return addFilesToContainer(navigator, signatureContainerDataSource, validFiles);
                                    } else {
                                        navigator.execute(Transaction.pop());
                                        return Observable.empty();
                                    }
                                })
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe();
                        sivaConfirmationDialog.positiveButtonClicks()
                                .flatMap(next -> {
                                    sivaConfirmationDialog.dismiss();
                                    return addFilesToContainer(navigator, signatureContainerDataSource, validFiles);
                                })
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe();
                    } else {
                        return addFilesToContainer(navigator, signatureContainerDataSource, validFiles);
                    }
                    return Observable.just(Result.ChooseFilesResult.create());
                });
    }
}
