package ee.ria.DigiDoc.android.signature.create;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

import android.app.Application;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResultException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ChooseFilesAction, Result.ChooseFilesResult>
            chooseFiles;

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
                        ImmutableList<FileStream> addedData = parseGetContentIntent(application.getContentResolver(), activityResult.data());
                        ImmutableList<FileStream> validFiles = FileSystem.getFilesWithValidSize(addedData);
                        ToastUtil.handleEmptyFileError(addedData, validFiles, application);
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
                                    Toast.makeText(application, Activity.getContext().get().getString(R.string.signature_create_error),
                                            Toast.LENGTH_LONG)
                                            .show();

                                    navigator.execute(Transaction.pop());
                                })
                                .map(containerAdd -> Result.ChooseFilesResult.create());
                    } else {
                        navigator.execute(Transaction.pop());
                        return Observable.just(Result.ChooseFilesResult.create());
                    }
                })
                .onErrorResumeNext(throwable -> {
                    ToastUtil.showEmptyFileError(application);
                    navigator.execute(Transaction.pop());
                });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ChooseFilesAction.class).compose(chooseFiles)));
    }
}
