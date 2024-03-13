package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;
import ee.ria.DigiDoc.sign.Signature;

public final class SignatureRoleScreen extends ConductorScreen {

    private static Signature userSignature;

    public static SignatureRoleScreen create(Signature signature) {
        userSignature = signature;
        return new SignatureRoleScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureRoleScreen() {
        super(R.id.signatureRoleScreen);
    }

    @Override
    protected View view(Context context) {
        return new SignatureRoleDetailsView(context, userSignature);
    }
}
