package ee.ria.DigiDoc.android.main.about;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class AboutScreen extends ConductorScreen {

    public static AboutScreen create() {
        return new AboutScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public AboutScreen() {
        super(R.id.mainAboutScreen);
    }

    @Override
    protected View view(Context context) {
        TextView view = new TextView(context);
        view.setText(R.string.main_home_menu_about);
        view.setGravity(Gravity.CENTER);
        return view;
    }
}
