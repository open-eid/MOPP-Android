package ee.ria.DigiDoc.configuration;

import java.text.SimpleDateFormat;
import java.util.Locale;

public final class ConfigurationDateUtil {

    public static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
    }
}
