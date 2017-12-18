package ee.ria.DigiDoc.android.main.settings;

import android.app.Application;
import android.content.res.Resources;
import android.support.v7.preference.PreferenceManager;

import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;

public final class SettingsDataStore {

    private final Application application;
    private final Resources resources;

    @Inject
    SettingsDataStore(Application application) {
        this.application = application;
        this.resources = application.getResources();
    }

    public String getFileType() {
        String key = resources.getString(R.string.main_settings_file_type_key);
        return PreferenceManager.getDefaultSharedPreferences(application).getString(key,
                resources.getString(R.string.main_settings_file_type_asice_key));
    }

    public ImmutableSet<String> getFileTypes() {
        return ImmutableSet.copyOf(resources.getStringArray(
                R.array.main_settings_file_type_entry_values));
    }
}
