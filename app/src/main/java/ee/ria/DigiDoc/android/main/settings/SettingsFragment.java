package ee.ria.DigiDoc.android.main.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import ee.ria.DigiDoc.R;

public final class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_settings, null);
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        View preferenceView = getPreferenceView();
        if (preferenceView instanceof RecyclerView) {
            RecyclerView preferenceRecyclerView = (RecyclerView) preferenceView;
            preferenceRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < preferenceRecyclerView.getChildCount(); i++) {
                        View settingView = preferenceRecyclerView.getChildAt(i);
                        ViewCompat.setAccessibilityDelegate(settingView, new AccessibilityDelegateCompat() {
                            @Override
                            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                                super.onInitializeAccessibilityNodeInfo(host, info);
                                if (host.getId() == R.id.mainSettingsAccessToSigningService) {
                                    info.setContentDescription(
                                            getAccessibilityDescription(R.string.main_settings_uuid_title, R.string.main_settings_uuid_key)
                                    );
                                } else if (host.getId() == R.id.mainSettingsAccessToTimeStampingService) {
                                    info.setContentDescription(
                                            getAccessibilityDescription(R.string.main_settings_tsa_url_title, R.string.main_settings_tsa_url_key)
                                    );
                                }
                            }
                        });
                    }
                }
            });
        }
        return new PreferenceGroupAdapter(preferenceScreen);
    }

    private View getPreferenceView() {
        View view = getView();
        if (view instanceof LinearLayout) {
            LinearLayout linearLayoutView = ((LinearLayout) view);
            if (linearLayoutView.getChildCount() > 0) {
                View linearChildView = linearLayoutView.getChildAt(0);
                if (linearChildView instanceof FrameLayout) {
                    FrameLayout frameLayoutView = ((FrameLayout) linearChildView);
                    if (frameLayoutView.getChildCount() > 0) {
                        View frameChildView = frameLayoutView.getChildAt(0);
                        if (frameChildView instanceof RecyclerView) {
                            return (frameChildView);
                        }
                    }
                }
            }
        }

        return view;
    }

    private String getAccessibilityDescription(@StringRes int titleId, @StringRes int keyId) {
        String summary = "";
        Preference preference = findPreference(getString(keyId));
        if (preference != null && preference.getSummary() != null) {
            summary = preference.getSummary().toString();
        }

        return getString(titleId) + " " + summary + " " + Button.class.getSimpleName();
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
