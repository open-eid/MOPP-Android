package ee.ria.DigiDoc.android.main.settings;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.preference.PreferenceManager;

import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;

public final class SettingsDataStore {

    private final SharedPreferences preferences;
    private final Resources resources;

    @Inject SettingsDataStore(Application application) {
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        this.resources = application.getResources();
    }

    public String getFileType() {
        return preferences.getString(resources.getString(R.string.main_settings_file_type_key),
                resources.getString(R.string.main_settings_file_type_asice_key));
    }

    public ImmutableSet<String> getFileTypes() {
        return ImmutableSet.copyOf(resources.getStringArray(
                R.array.main_settings_file_type_entry_values));
    }

    public String getPhoneNo() {
        return preferences.getString(resources.getString(R.string.main_settings_phone_no_key), "");
    }

    public void setPhoneNo(String phoneNo) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_phone_no_key), phoneNo);
        editor.apply();
    }

    public String getPersonalCode() {
        return preferences.getString(resources.getString(R.string.main_settings_personal_code_key),
                "");
    }

    public void setPersonalCode(String personalCode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_personal_code_key),
                personalCode);
        editor.apply();
    }
}
