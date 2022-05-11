package ee.ria.DigiDoc.android.signature.detail;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;

public final class SignatureDetailScreen extends ConductorScreen {

    private static final int SIGNATURE_DETAIL_SCREEN_ID = R.id.signatureDetailScreen;
    private Signature userSignature;
    private SignedContainer userSignedContainer;

    public SignatureDetailScreen(int id, Signature signature, SignedContainer signedContainer) {
        super(id);
        this.userSignature = signature;
        this.userSignedContainer = signedContainer;
    }

    public static SignatureDetailScreen create(Signature signature, SignedContainer signedContainer) {
        return new SignatureDetailScreen(SIGNATURE_DETAIL_SCREEN_ID, signature, signedContainer);
    }

    @SuppressWarnings("WeakerAccess")
    public SignatureDetailScreen() {
        super(SIGNATURE_DETAIL_SCREEN_ID);
    }

    @Override
    protected View view(Context context) {
        return new SignatureDetailView(context, userSignature, userSignedContainer);
    }
}
