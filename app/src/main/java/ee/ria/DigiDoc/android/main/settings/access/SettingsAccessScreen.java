package ee.ria.DigiDoc.android.main.settings.access;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SettingsAccessScreen extends ConductorScreen {

    public static SettingsAccessScreen create() {
        return new SettingsAccessScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SettingsAccessScreen() {
        super(R.id.mainSettingsAccessScreen);
    }

    @Override
    protected View view(Context context) {
        return new SettingsAccessView(context);
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        FragmentActivity activity = (FragmentActivity) getActivity();
        if (activity != null) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.mainSettingsFragment);
            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss();
            }
        }
        super.onDestroyView(view);
    }
}
