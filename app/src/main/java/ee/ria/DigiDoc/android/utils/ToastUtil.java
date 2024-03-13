package ee.ria.DigiDoc.android.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import timber.log.Timber;

public final class ToastUtil {

    public static void handleEmptyFileError(ImmutableList<FileStream> validFiles,
                                            Context context) throws EmptyFileException {
        if (validFiles.isEmpty()) {
            Timber.log(Log.ERROR, "Add file to container failed");
            throw new EmptyFileException();
        }
        if (FileSystem.isEmptyFileInList(validFiles)) {
            showEmptyFileError(context);
        }
    }

    public static void showEmptyFileError(Context context) {
        Timber.log(Log.DEBUG, "Excluded empty files in list");
        new Handler(Looper.getMainLooper()).post(() ->
                showError(context, R.string.empty_file_error));
    }

    public static void showError(Context context, @StringRes int message) {
        Toast.makeText(context, context.getString(message),
                Toast.LENGTH_LONG)
                .show();
    }
}
