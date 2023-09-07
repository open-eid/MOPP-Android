package ee.ria.DigiDoc.android.utils;

import android.os.Bundle;

import java.io.File;

public final class BundleUtils {

    private BundleUtils() {}

    public static void putFile(Bundle bundle, String key, File value) {
        bundle.putString(key, value.getAbsolutePath());
    }

    @SuppressWarnings("ConstantConditions")
    public static File getFile(Bundle bundle, String key) {
        return new File(bundle.getString(key));
    }

    public static void putBoolean(Bundle bundle, String key, boolean value) {
        bundle.putBoolean(key, value);
    }

}
