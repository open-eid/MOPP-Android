package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class HomeScreen extends ConductorScreen {

    private static final String KEY_INTENT = "intent";

    public static HomeScreen create(Intent intent) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_INTENT, intent);
        return new HomeScreen(args);
    }

    private final Intent intent;

    @SuppressWarnings("WeakerAccess")
    public HomeScreen(Bundle args) {
        super(R.id.mainHomeScreen, args);
        intent = args.getParcelable(KEY_INTENT);
    }

    @Override
    protected View view(Context context) {
        return new HomeView(context, intent, getInstanceId());
    }
}
