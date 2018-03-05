package ee.ria.DigiDoc.android.main.help;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class HelpScreen extends ConductorScreen {

    public static HelpScreen create() {
        return new HelpScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public HelpScreen() {
        super(R.id.mainHelpScreen);
    }

    @Override
    protected View view(Context context) {
        TextView view = new TextView(context);
        view.setText(R.string.main_home_menu_help);
        view.setGravity(Gravity.CENTER);
        return view;
    }
}
