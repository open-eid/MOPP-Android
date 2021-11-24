package ee.ria.DigiDoc.android.utils;

import static com.google.common.io.Files.getFileExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
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
        for (FileStream file : files) {
            String extension = getFileExtension(FilenameUtils.getName(file.displayName())).toLowerCase(Locale.US);
            if (SEND_SIVA_CONTAINER_NOTIFICATION_EXTENSIONS.contains(extension) || ("pdf".equals(extension) &&
                    SignedContainer.isSignedPDFFile(file.source(), Activity.getContext().get(), file.displayName()))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSivaConfirmationNeeded(File containerFile, DataFile dataFile) {
        String extension = getFileExtension(dataFile.name()).toLowerCase(Locale.US);
        boolean isSignedPdfDataFile =
                extension.equals("pdf") && dataFile.name().equals(containerFile.getName());
        return isSignedPdfDataFile || SEND_SIVA_CONTAINER_NOTIFICATION_EXTENSIONS.contains(extension);
    }

    public static void showSivaConfirmationDialog(ConfirmationDialog sivaConfirmationDialog) {
        sivaConfirmationDialog.show();
        ClickableDialogUtil.makeLinksInDialogClickable(sivaConfirmationDialog);
    }

}
