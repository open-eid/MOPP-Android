package ee.ria.DigiDoc.android.signature.create;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

public final class SignatureCreateScreen extends ConductorScreen {

    public static SignatureCreateScreen create() {
        return new SignatureCreateScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureCreateScreen() {
        super(R.id.signatureCreateScreen);
    }

    @Override
    protected View createView(Context context) {
        return new SignatureCreateView(context);
    }
}
