package ee.ria.DigiDoc.android.main.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

public final class SettingsScreen extends ConductorScreen {

    public static SettingsScreen create() {
        return new SettingsScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SettingsScreen() {
        super(R.id.mainSettingsScreen);
    }

    @Override
    protected View createView(Context context) {
        return new SettingsView(context);
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
