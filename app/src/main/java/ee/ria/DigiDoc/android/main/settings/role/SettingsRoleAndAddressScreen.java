package ee.ria.DigiDoc.android.main.settings.role;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.access.SettingsAccessView;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SettingsRoleAndAddressScreen extends ConductorScreen {

    public static SettingsRoleAndAddressScreen create() {
        return new SettingsRoleAndAddressScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SettingsRoleAndAddressScreen() {
        super(R.id.mainSettingsRoleAndAddressScreen);
    }

    @Override
    protected View view(Context context) {
        return new SettingsRoleAndAddressView(context);
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
