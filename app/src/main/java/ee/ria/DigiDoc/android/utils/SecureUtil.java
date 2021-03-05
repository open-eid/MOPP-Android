package ee.ria.DigiDoc.android.utils;

import android.view.Window;
import android.view.WindowManager;

import ee.ria.DigiDoc.BuildConfig;

public final class SecureUtil {

    private SecureUtil() {}

    public static void markAsSecure(Window window) {
        if (shouldMarkAsSecure()) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private static boolean shouldMarkAsSecure() {
        return !BuildConfig.BUILD_TYPE.contentEquals("debug");
    }

}
