package ee.ria.DigiDoc.android.main.sharing;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SharingScreen extends ConductorScreen {

    public static SharingScreen create() {
        return new SharingScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SharingScreen() {
        super(R.id.mainDiagnosticsScreen);
    }

    @Override
    protected View view(Context context) {
        return new SharingScreenView(context);
    }
}
