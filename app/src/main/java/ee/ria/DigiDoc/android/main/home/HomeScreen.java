package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

public final class HomeScreen extends ConductorScreen {

    public static HomeScreen create() {
        return new HomeScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public HomeScreen() {
        super(R.id.mainHomeScreen);
    }

    @Override
    protected View createView(Context context) {
        return new HomeView(context);
    }
}
