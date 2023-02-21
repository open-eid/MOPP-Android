package ee.ria.DigiDoc.android.main.diagnostics;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSaveIntent;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.main.diagnostics.source.DiagnosticsDataSource;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.FileUtil;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Intent, Result> {

    private static final int SAVE_FILE = 1;
    private static final int SAVE_LOGS_FILE = 2;

    private final ObservableTransformer<Intent.InitialIntent, Result.InitialResult> initial;
    private final ObservableTransformer<Intent.DiagnosticsSaveIntent, Result> diagnosticsSave;
    private final ObservableTransformer<Intent.DiagnosticsLogsSaveIntent, Result> diagnosticsLogsSave;

    @Inject Processor(Navigator navigator,
              ContentResolver contentResolver,
              DiagnosticsDataSource diagnosticsDataSource) {

        initial = upstream -> upstream.switchMap(action -> Observable.just(Result.InitialResult.activity()));

        diagnosticsSave = upstream -> upstream.switchMap(action -> {
            if (action.diagnosticsFile() == null) {
                Timber.log(Log.ERROR, "Unable to get diagnostics file");
                Toast.makeText(navigator.activity(), navigator.activity().getString(R.string.file_saved_error),
                        Toast.LENGTH_LONG).show();
                return Observable.just(Result.DiagnosticsSaveResult.failure(new EmptyFileException()));
            }
            navigator.execute(Transaction.activityForResult(SAVE_FILE,
                    createSaveIntent(action.diagnosticsFile(), contentResolver), null));
            return navigator.activityResults()
                    .filter(activityResult ->
                            activityResult.requestCode() == SAVE_FILE)
                    .switchMap(activityResult -> diagnosticsDataSource
                            .get(action.diagnosticsFile())
                            .toObservable()
                            .map(documentFile -> {
                                if (activityResult.resultCode() == RESULT_OK) {
                                    android.content.Intent dataIntent = activityResult.data();
                                    if (dataIntent != null) {
                                        Uri data = dataIntent.getData();
                                        return saveFile(documentFile, data,
                                                action.diagnosticsFile().toPath(),
                                                (Activity) navigator.activity(), false);
                                    }
                                }
                                return Result.DiagnosticsSaveResult.cancel();
                            })
                            .onErrorReturn(Result.DiagnosticsSaveResult::failure)
                            .startWithItem(Result.DiagnosticsSaveResult.activity()));
        });

        diagnosticsLogsSave = upstream -> upstream.switchMap(action -> {
            if (action.logFile() == null) {
                Timber.log(Log.ERROR, "Unable to get diagnostics logs files");
                Toast.makeText(navigator.activity(), navigator.activity().getString(R.string.file_saved_error),
                        Toast.LENGTH_LONG).show();
                return Observable.just(Result.DiagnosticsSaveResult.failure(new EmptyFileException()));
            }
            navigator.execute(Transaction.activityForResult(SAVE_LOGS_FILE,
                    createSaveIntent(action.logFile(), contentResolver), null));
            return navigator.activityResults()
                    .filter(activityResult ->
                            activityResult.requestCode() == SAVE_LOGS_FILE)
                    .switchMap(activityResult -> diagnosticsDataSource
                            .get(action.logFile())
                            .toObservable()
                            .map(documentFile -> {
                                if (activityResult.resultCode() == RESULT_OK) {
                                    android.content.Intent dataIntent = activityResult.data();
                                    if (dataIntent != null) {
                                        Uri data = dataIntent.getData();
                                        return saveFile(documentFile, data,
                                                action.logFile().toPath(),
                                                (Activity) navigator.activity(), true);
                                    }
                                }
                                return Result.DiagnosticsSaveResult.cancel();
                            })
                            .onErrorReturn(Result.DiagnosticsSaveResult::failure)
                            .startWithItem(Result.DiagnosticsSaveResult.activity()));
        });
    }

    private Result.DiagnosticsSaveResult saveFile(File documentFile, Uri data, Path logFilePath,
                                                  Activity activity, boolean isDiagnosticsLogsFile) {
        if (data == null) {
            Toast.makeText(Activity.getContext().get(), Activity.getContext().get().getString(R.string.file_saved_error),
                    Toast.LENGTH_LONG).show();
            return Result.DiagnosticsSaveResult.failure(new IllegalArgumentException("No data to save"));
        }

        try (
                InputStream inputStream = new FileInputStream(documentFile);
                OutputStream outputStream = activity.getApplicationContext().getContentResolver().openOutputStream(data)
        ) {
            ByteStreams.copy(inputStream, outputStream);
            boolean isTempFileDeleted = Files.deleteIfExists(logFilePath);
            if (!isTempFileDeleted) {
                Timber.log(Log.ERROR, "Unable to delete temporary diagnostics or logs file or file does not exist");
            }

            if (isDiagnosticsLogsFile) {
                activity.getSettingsDataStore().setIsLogFileGenerationEnabled(false);
                activity.getSettingsDataStore().setIsLogFileGenerationRunning(false);
                removeLogFiles(activity.getApplicationContext());
            }

            Toast.makeText(activity, activity.getString(R.string.file_saved),
                    Toast.LENGTH_LONG).show();

            if (isDiagnosticsLogsFile) {
                activity.restartAppWithIntent(activity.getIntent(), true);
            }

        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Unable to save diagnostics or logs file");
            return Result.DiagnosticsSaveResult.failure(e);
        }
        return Result.DiagnosticsSaveResult.success();
    }

    private void removeLogFiles(Context context) throws IOException {
        File logsDirectory = FileUtil.getLogsDirectory(context);
        if (FileUtil.logsExist(logsDirectory)) {
            File[] files = logsDirectory.listFiles() != null ? logsDirectory.listFiles() : new File[]{};
            if (files != null) {
                for (File file : files) {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
    }

    @Override
    public @NonNull ObservableSource<Result> apply(@NonNull Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.DiagnosticsSaveIntent.class).compose(diagnosticsSave),
                shared.ofType(Intent.DiagnosticsLogsSaveIntent.class).compose(diagnosticsLogsSave)
        ));
    }
}
