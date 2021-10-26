package ee.ria.DigiDoc.android.utils;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import timber.log.Timber;

public final class ToastUtil {

    public static void handleEmptyFileError(ImmutableList<FileStream> fileStreams, ImmutableList<FileStream> validFiles,
                                            Application application) throws EmptyFileException {
        if (validFiles.isEmpty()) {
            Timber.e("Add file to container failed");
            throw new EmptyFileException();
        }
        if (FileSystem.isEmptyFileInList(fileStreams)) {
            showEmptyFileError(application);
        }
    }

    public static void showEmptyFileError(Context context) {
        Timber.d("Excluded empty files in list");
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, Activity.getContext().get().getString(R.string.empty_file_error),
                Toast.LENGTH_LONG)
                .show());

    }

    public static void showGeneralError(Context context) {
        Toast.makeText(context, Activity.getContext().get().getString(R.string.signature_create_error),
                Toast.LENGTH_LONG)
                .show();
    }
}
