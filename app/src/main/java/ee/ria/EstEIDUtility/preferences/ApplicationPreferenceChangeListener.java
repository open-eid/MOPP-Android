package ee.ria.EstEIDUtility.preferences;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import ee.ria.EstEIDUtility.R;

class ApplicationPreferenceChangeListener implements Preference.OnPreferenceChangeListener {

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        if (isListPreference(preference)) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

        } else if (isRingtonePreference(preference)) {
            if (TextUtils.isEmpty(stringValue)) {
                preference.setSummary(R.string.pref_ringtone_silent);
            } else {
                Ringtone ringtone = getRingtone(preference, stringValue);
                if (ringtone == null) {
                    preference.setSummary(null);
                } else {
                    preference.setSummary(getRingtoneTitle(preference, ringtone));
                }
            }

        } else {
            preference.setSummary(stringValue);
        }
        return true;
    }

    private String getRingtoneTitle(Preference preference, Ringtone ringtone) {
        return ringtone.getTitle(preference.getContext());
    }

    private Ringtone getRingtone(Preference preference, String stringValue) {
        return RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));
    }

    private boolean isRingtonePreference(Preference preference) {
        return preference instanceof RingtonePreference;
    }

    private boolean isListPreference(Preference preference) {
        return preference instanceof ListPreference;
    }
}
