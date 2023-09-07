package ee.ria.DigiDoc.android.main.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SettingsScreen extends ConductorScreen {

    public static SettingsScreen create() {
        return new SettingsScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SettingsScreen() {
        super(R.id.mainSettingsScreen);
    }

    @Override
    protected View view(Context context) {
        return new SettingsView(context);
    }
}
