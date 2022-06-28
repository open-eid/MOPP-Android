package ee.ria.DigiDoc.android.utils;

import android.util.Log;

import java.io.File;

import ee.ria.DigiDoc.sign.SignedContainer;
import timber.log.Timber;

public class ContainerMimeTypeUtil {
    public static String getContainerExtension(File file) {
        String containerMimeType = "";
        try {
            containerMimeType = SignedContainer.getMediaType(file);
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Unable to get media type from container");
            return "";
        }
        return mimeTypeToExtension(containerMimeType);
    }

    private static String mimeTypeToExtension(String mimetype) {
        switch (mimetype) {
            case "application/x-cdoc":
                return "cdoc";
            case "application/x-p12d":
                return "p12";
            case "application/vnd.lt.archyvai.adoc-2008":
                return "adoc";
            case "application/x-ddoc":
                return "ddoc";
            case "application/vnd.etsi.asic-s+zip":
                return "asics";
            case "application/vnd.etsi.asic-e+zip":
                return "asice";
            case "application/pdf":
                return "pdf";
            default:
                return "";
        }
    }
}
