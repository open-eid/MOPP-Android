package ee.ria.DigiDoc.android.signature.home;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

public final class SignatureHomeScreen extends ConductorScreen {

    public static SignatureHomeScreen create() {
        return new SignatureHomeScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureHomeScreen() {
        super(R.id.signatureHomeScreen);
    }

    @Override
    protected View createView(Context context) {
        return new SignatureHomeView(context);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof SignatureHomeScreen;
    }
}
