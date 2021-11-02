package ee.ria.DigiDoc.android.main.settings;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import ee.ria.DigiDoc.R;

public final class SettingsFragment extends PreferenceFragmentCompat {

    private final Preference.OnPreferenceChangeListener summaryChangeListener
            = (preference, newValue) -> {
                    CharSequence summary;
                    if (preference instanceof ListPreference) {
                        ListPreference listPreference = (ListPreference) preference;
                        summary = getListPreferenceEntry(listPreference.getEntryValues(),
                                listPreference.getEntries(), newValue);
                    } else {
                        summary = (CharSequence) newValue;
                    }
                    preference.setSummary(summary);
                    return true;
            };

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_settings, null);
//        bindSummary(R.string.main_settings_phone_no_key);
//        bindSummary(R.string.main_settings_personal_code_key);
//        bindSummary(R.string.main_settings_uuid_key);
//        bindSummary(R.string.main_settings_role_key);
//        bindSummary(R.string.main_settings_city_key);
//        bindSummary(R.string.main_settings_county_key);
//        bindSummary(R.string.main_settings_country_key);
//        bindSummary(R.string.main_settings_postal_code_key);
    }

    private void bindSummary(@StringRes int key) {
        String preferenceKey = getString(key);
        Preference preference = findPreference(preferenceKey);
        if (preference != null) {
            preference.setOnPreferenceChangeListener(summaryChangeListener);
            preference.callChangeListener(PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getString(preferenceKey, null));
        }
    }

    @Nullable
    private static CharSequence getListPreferenceEntry(CharSequence[] entryValues,
                                                       CharSequence[] entries, Object newValue) {
        CharSequence value = (CharSequence) newValue;
        for (int i = 0; i < entryValues.length; i++) {
            if (TextUtils.equals(entryValues[i], value)) {
                return entries[i];
            }
        }
        return null;
    }


    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof TsaUrlPreference) {
            displayPreferenceDialog(new TsaUrlPreferenceDialogFragment(), preference.getKey());
        } else if (preference instanceof UUIDPreference) {
            displayPreferenceDialog(new UUIDPreferenceDialogFragment(), preference.getKey());
        }
    }
}
