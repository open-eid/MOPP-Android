package ee.ria.DigiDoc.android.signature.list;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SignatureListScreen extends ConductorScreen {

    public static SignatureListScreen create() {
        return new SignatureListScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureListScreen() {
        super(R.id.signatureListScreen);
    }

    @Override
    protected View view(Context context) {
        TextView view = new TextView(context);
        view.setText(R.string.main_home_menu_recent);
        view.setGravity(Gravity.CENTER);
        return view;
    }
}
