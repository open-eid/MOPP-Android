package ee.ria.DigiDoc.configuration.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.StringRes;

import java.util.Locale;

public class LocalizationUtil {

    public static String getLocalizedMessage(Context context, @StringRes int message) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(Locale.getDefault());
        return context.createConfigurationContext(configuration)
                .getText(message)
                .toString();
    }
}
