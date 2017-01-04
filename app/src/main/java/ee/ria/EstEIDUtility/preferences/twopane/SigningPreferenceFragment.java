package ee.ria.EstEIDUtility.preferences.twopane;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.preferences.SettingsActivity;

/**
 * This fragment shows data and sync preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SigningPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_signing);
        setHasOptionsMenu(true);

        bindPreferencesSummariesToValues(
                "container_file_type",
                "signature_role",
                "signature_resolution",
                "signature_location_city",
                "signature_location_state",
                "signature_location_country",
                "signature_location_zip"
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindPreferencesSummariesToValues(String... preferenceKey) {
        for (String key : preferenceKey) {
            SettingsActivity.bindPreferenceSummaryToValue(findPreference(key));
        }
    }
}
