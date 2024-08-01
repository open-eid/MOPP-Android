package ee.ria.DigiDoc.android.utils.container;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.StringRes;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;

public class ContainerUtil {
    public static void showExistingFilesMessage(Context context,
                                                ImmutableList<FileStream> existingFiles,
                                                @StringRes int oneFileExistsMessage,
                                                @StringRes int multipleFilesExistMessage) {
        new Handler(Looper.getMainLooper()).post(() -> {
            List<String> existingFileNames = new ArrayList<>();
            for (FileStream fileStream : existingFiles) {
                existingFileNames.add("'" + fileStream.displayName() + "'");
            }

            ErrorDialog errorDialog = new ErrorDialog(context);
            if (existingFileNames.size() > 1) {
                errorDialog.setMessage(context.getResources()
                        .getString(multipleFilesExistMessage, String.join(", ", existingFileNames)));
            } else {
                errorDialog.setMessage(context.getResources()
                        .getString(oneFileExistsMessage, String.join(", ", existingFileNames)));
            }
            errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources()
                    .getString(android.R.string.ok), (dialog, which) -> dialog.cancel());
            errorDialog.show();
        });
    }
}
