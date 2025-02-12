package ee.ria.DigiDoc.android.utils.container;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.StringRes;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static ImmutableList<FileStream> getUniqueFileNames(ImmutableList<FileStream> fileStreams) {
        return ImmutableList.copyOf(new ArrayList<>(
                fileStreams.stream()
                        .collect(Collectors.toMap(
                                FileStream::displayName,
                                file -> file,
                                (existing, duplicate) -> existing,
                                LinkedHashMap::new
                        ))
                        .values()
        ));
    }

    public static ImmutableList<FileStream> getDuplicateFiles(ImmutableList<FileStream> fileStreams) {
        Map<String, List<FileStream>> groupedFiles = fileStreams.stream()
                .collect(Collectors.groupingBy(FileStream::displayName, LinkedHashMap::new, Collectors.toList()));

        return ImmutableList.copyOf(groupedFiles.values().stream()
                .filter(files -> files.size() > 1)
                .map(files -> files.get(0))
                .collect(Collectors.toList()));
    }
}
