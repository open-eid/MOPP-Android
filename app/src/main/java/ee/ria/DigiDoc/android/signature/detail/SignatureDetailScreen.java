package ee.ria.DigiDoc.android.signature.detail;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;

public final class SignatureDetailScreen extends ConductorScreen {

    private static Signature userSignature;
    private static SignedContainer userSignedContainer;

    public static SignatureDetailScreen create(Signature signature, SignedContainer signedContainer) {
        userSignature = signature;
        userSignedContainer = signedContainer;
        return new SignatureDetailScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureDetailScreen() {
        super(R.id.signatureDetailScreen);
    }

    @Override
    protected View view(Context context) {
        return new SignatureDetailView(context, userSignature, userSignedContainer);
    }
}
