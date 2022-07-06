package ee.ria.DigiDoc.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class LoggingUtil {

    public static boolean isLoggingEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDiagnosticsLoggingEnabled = sharedPreferences.getBoolean(context.getString(R.string.main_diagnostics_logging_key), false);
        boolean isDiagnosticsLoggingRunning = sharedPreferences.getBoolean(context.getString(R.string.main_diagnostics_logging_running_key), false);

        return isDiagnosticsLoggingEnabled && isDiagnosticsLoggingRunning;
    }
}
