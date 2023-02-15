package ee.ria.DigiDoc.android.utils;

import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;

public final class SecureUtil {

    private SecureUtil() {}

    public static void markAsSecure(Context context, Window window) {
        if (shouldMarkAsSecure(context)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private static boolean shouldMarkAsSecure(Context context) {
        SettingsDataStore settingsDataStore = ((Activity) context).getSettingsDataStore();
        boolean isScreenshotAllowed = settingsDataStore.getIsScreenshotAllowed();
        if (BuildConfig.BUILD_TYPE.contentEquals("debug")) {
            return false;
        }
        return !isScreenshotAllowed;
    }

}
