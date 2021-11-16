package ee.ria.DigiDoc.android.signature.create;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

import android.app.Application;

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
import ee.ria.DigiDoc.android.utils.navigator.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResultException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.common.ActivityUtil;
import ee.ria.DigiDoc.common.FileUtil;
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

    private final ConfirmationDialog sivaConfirmationDialog = new ConfirmationDialog(Activity.getContext().get(),
            R.string.siva_send_message_dialog, R.id.sivaConfirmationDialog);

    private static final ImmutableSet<String> SEND_SIVA_CONTAINER_NOTIFICATION_EXTENSIONS = ImmutableSet.<String>builder()
            .add("ddoc", "asics", "scs")
            .build();

    @Inject Processor(Navigator navigator,
                      SignatureContainerDataSource signatureContainerDataSource,
                      Application application) {
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
                        ImmutableList<FileStream> validFiles = FileSystem.getFilesWithValidSize(
                                parseGetContentIntent(application.getContentResolver(), activityResult.data()));
                        ToastUtil.handleEmptyFileError(validFiles, application);
                        if (SivaUtil.isSivaConfirmationNeeded(validFiles)) {
                            sivaConfirmationDialog.show();
                            ClickableDialogUtil.makeLinksInDialogClickable(sivaConfirmationDialog);
                            sivaConfirmationDialog.cancels()
                                    .doOnNext(next -> navigator.execute(Transaction.pop()))
                                    .subscribe();
                            sivaConfirmationDialog.positiveButtonClicks()
                                    .flatMap(next -> {
                                        sivaConfirmationDialog.dismiss();
                                        return addFilesToContainer(navigator, signatureContainerDataSource, application, validFiles);
                                    })
                                    .subscribe();
                            return Observable.just(Result.ChooseFilesResult.create());
                        } else {
                            return addFilesToContainer(navigator, signatureContainerDataSource, application, validFiles);
                        }
                    } else {
                        if (ActivityUtil.isExternalFileOpened(navigator.activity())) {
                            ActivityUtil.restartActivity(application.getApplicationContext(), navigator.activity());
                        } else {
                            navigator.execute(Transaction.pop());
                        }
                        return Observable.just(Result.ChooseFilesResult.create());
                    }
                })
                .onErrorReturn(throwable -> {
                    List<Throwable> exceptions = ((CompositeException) throwable).getExceptions();
                    if (!exceptions.isEmpty()) {
                        boolean isEmptyFileException = exceptions.stream().anyMatch(exception ->
                                (exception instanceof EmptyFileException));
                        if (isEmptyFileException) {
                            ToastUtil.showEmptyFileError(Activity.getContext().get());
                        } else {
                            ToastUtil.showGeneralError(Activity.getContext().get());
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
                                                                     Application application,
                                                                     ImmutableList<FileStream> validFiles) {
        return signatureContainerDataSource
                .addContainer(validFiles, false)
                .toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(containerAdd ->
                        navigator.execute(Transaction.replace(SignatureUpdateScreen
                                .create(containerAdd.isExistingContainer(), false,
                                        containerAdd.containerFile(), false,
                                        false))))
                .doOnError(throwable1 -> {
                    Timber.d(throwable1, "Add signed container failed");
                    ToastUtil.showGeneralError(application);

                    navigator.execute(Transaction.pop());
                })
                .map(containerAdd -> Result.ChooseFilesResult.create());
    }
}
