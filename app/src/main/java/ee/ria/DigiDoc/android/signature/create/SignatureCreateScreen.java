package ee.ria.DigiDoc.android.signature.create;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SignatureCreateScreen extends ConductorScreen {

    public static SignatureCreateScreen create() {
        return new SignatureCreateScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureCreateScreen() {
        super(R.id.signatureCreateScreen);
    }

    @Override
    protected View view(Context context) {
        return new SignatureCreateView(context);
    }
}
