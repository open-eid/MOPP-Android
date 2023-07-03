package ee.ria.DigiDoc.android.main.settings.create;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_TSA_CERT;

import android.app.Application;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.google.common.collect.ImmutableList;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigator.ActivityResultException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.ActivityUtil;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.exceptions.CompositeException;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ChooseFileAction, Result.ChooseFileResult>
            chooseFile;

    @Inject Processor(Navigator navigator,
                      Application application,
                      SettingsDataStore settingsDataStore,
                      FileSystem fileSystem) {

        chooseFile = upstream -> upstream
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
                            .map(activityResult -> Result.ChooseFileResult.create(navigator.activity()));
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

                            try (InputStream initialStream = application.getContentResolver().openInputStream(activityResult.data().getData())) {
                                DocumentFile documentFile = DocumentFile.fromSingleUri(navigator.activity(), activityResult.data().getData());
                                if (documentFile != null) {
                                    File tsaCertFolder = new File(navigator.activity().getFilesDir(), DIR_TSA_CERT);
                                    if (!tsaCertFolder.exists()) {
                                        boolean isFolderCreated = tsaCertFolder.mkdirs();
                                        Timber.log(Log.DEBUG, String.format("TSA cert folder created: %s", isFolderCreated));
                                    }

                                    String fileName = documentFile.getName();
                                    if (fileName == null || fileName.isEmpty()) {
                                        fileName = "tsaCert";
                                    }
                                    File tsaFile = new File(tsaCertFolder, fileName);

                                    FileUtils.copyInputStreamToFile(initialStream, tsaFile);

                                    settingsDataStore.setTSACertName(tsaFile.getName());

                                    return Observable.just(Result.ChooseFileResult.create(navigator.activity()));
                                }
                            } catch (Exception e) {
                                Timber.log(Log.ERROR, e, "Unable to read TSA certificate file data");
                            }

                            return Observable.just(Result.ChooseFileResult.create(navigator.activity()));
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
                        return Observable.just(Result.ChooseFileResult.create(navigator.activity()));
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
                    return Result.ChooseFileResult.create(navigator.activity());
                });
    }

    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ChooseFileAction.class).compose(chooseFile)));
    }
}
