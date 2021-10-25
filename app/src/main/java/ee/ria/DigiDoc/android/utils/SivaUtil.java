package ee.ria.DigiDoc.android.utils;

import static com.google.common.io.Files.getFileExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Locale;

import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.SignedContainer;

public class SivaUtil {

    private static final ImmutableSet<String> SEND_SIVA_CONTAINER_NOTIFICATION_EXTENSIONS = ImmutableSet.<String>builder()
            .add("ddoc", "asics", "scs")
            .build();

    public static boolean isSivaConfirmationNeeded(ImmutableList<FileStream> files) {
        return files.stream().anyMatch(fileStream -> {
            String extension = getFileExtension(fileStream.displayName()).toLowerCase(Locale.US);
            return "pdf".equals(extension) ?
                    SignedContainer.isSignedPDFFile(fileStream.source(), Activity.getContext().get(), fileStream.displayName()) :
                    SEND_SIVA_CONTAINER_NOTIFICATION_EXTENSIONS.contains(extension);
        });
    }

    public static boolean isSivaConfirmationNeeded(DataFile dataFile) {
        String extension = getFileExtension(dataFile.name()).toLowerCase(Locale.US);
        boolean isSignedPdfDataFile =
                getFileExtension(dataFile.name()).toLowerCase(Locale.US)
                        .equals("pdf")
                        && dataFile.name().equals(dataFile.name());
        return isSignedPdfDataFile || SEND_SIVA_CONTAINER_NOTIFICATION_EXTENSIONS.contains(extension);
    }

    public static void showSivaConfirmationDialog(ConfirmationDialog sivaConfirmationDialog) {
        sivaConfirmationDialog.show();
        ClickableDialogUtil.makeLinksInDialogClickable(sivaConfirmationDialog);
    }

}