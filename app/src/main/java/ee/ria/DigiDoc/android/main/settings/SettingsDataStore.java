package ee.ria.DigiDoc.android.main.settings;

import android.app.Application;
import android.support.v7.preference.PreferenceManager;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;

/**
 * Should model it with MVI.
 */
public final class SettingsDataStore {

    private final Application application;

    @Inject
    SettingsDataStore(Application application) {
        this.application = application;
    }

    public String getFileType() {
        String key = application.getString(R.string.main_settings_file_type_key);
        return PreferenceManager.getDefaultSharedPreferences(application).getString(key,
                application.getString(R.string.main_settings_file_type_asice_key));
    }
}
