package ee.ria.DigiDoc.android.main.accessibility;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public class AccessibilityScreen extends ConductorScreen {

    public static AccessibilityScreen create() {
        return new AccessibilityScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public AccessibilityScreen() {
        super(R.id.mainAccessibilityScreen);
    }

    @Override
    protected View view(Context context) {
        return new AccessibilityView(context);
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        FragmentActivity activity = (FragmentActivity) getActivity();
        if (activity != null) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.mainAccessibilityFragment);
            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss();
            }
        }
        super.onDestroyView(view);
    }
}
