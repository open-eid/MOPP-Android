package ee.ria.DigiDoc.android.main.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;

import ee.ria.DigiDoc.R;

public final class SettingsFragment extends PreferenceFragmentCompatDividers {

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
        bindSummary(R.string.main_settings_phone_no_key);
        bindSummary(R.string.main_settings_personal_code_key);
        bindSummary(R.string.main_settings_role_key);
        bindSummary(R.string.main_settings_city_key);
        bindSummary(R.string.main_settings_county_key);
        bindSummary(R.string.main_settings_country_key);
        bindSummary(R.string.main_settings_postal_code_key);
        bindSummary(R.string.main_settings_signature_profile_key);
    }

    private void bindSummary(@StringRes int key) {
        String preferenceKey = getString(key);
        Preference preference = findPreference(preferenceKey);
        preference.setOnPreferenceChangeListener(summaryChangeListener);
        preference.callChangeListener(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(preferenceKey, null));
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
}
