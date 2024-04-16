package ee.ria.DigiDoc.android.main.settings.signing;

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
        setPreferencesFromResource(R.xml.main_settings_signing, null);
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
                                    info.setContentDescription("");
                                    info.setText("");
                                } else if (host.getId() == R.id.mainSettingsAccessToTimeStampingService) {
                                    info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                                    info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SELECT);
                                    info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_FOCUS);

                                    info.setContentDescription("");
                                    info.setText("");
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

    private String getAccessibilityDescription(@StringRes int titleId) {
        return String.format("%s %s", getString(titleId), Button.class.getSimpleName());
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
