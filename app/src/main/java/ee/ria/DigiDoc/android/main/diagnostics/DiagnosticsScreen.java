package ee.ria.DigiDoc.android.main.diagnostics;

import android.content.Context;
import android.view.View;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public class DiagnosticsScreen extends ConductorScreen {

    public static DiagnosticsScreen create() {
        return new DiagnosticsScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public DiagnosticsScreen() {
        super(R.id.mainAboutScreen);
    }

    @Override
    protected View view(Context context) {
        return new DiagnosticsView(context);
    }
}
