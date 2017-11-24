package ee.ria.DigiDoc.android.signature.container;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

public final class SignatureContainerScreen extends ConductorScreen {

    public static SignatureContainerScreen create() {
        return new SignatureContainerScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureContainerScreen() {
        super(R.id.signatureContainerScreen);
    }

    @Override
    protected View createView(Context context) {
        return new SignatureContainerView(context);
    }
}
