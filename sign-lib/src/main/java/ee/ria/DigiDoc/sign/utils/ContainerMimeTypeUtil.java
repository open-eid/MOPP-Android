package ee.ria.DigiDoc.sign.utils;

import android.util.Log;

import com.google.common.io.Files;

import java.io.File;

import ee.ria.DigiDoc.sign.SignedContainer;
import timber.log.Timber;

public class ContainerMimeTypeUtil {
    public static String getContainerExtension(File file) {
        try {
            return Files.getFileExtension(file.getName());
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Unable to get file extension");
            return "";
        }
    }
}
