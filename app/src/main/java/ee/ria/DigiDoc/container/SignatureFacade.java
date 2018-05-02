package ee.ria.DigiDoc.container;

import ee.ria.libdigidocpp.Signature;
import timber.log.Timber;

public class SignatureFacade {

    private static final String TAG = SignatureFacade.class.getName();
    private final Signature signature;

    SignatureFacade(Signature signature) {
        this.signature = signature;
        Timber.tag(TAG);
    }

    public String getId() {
        return signature.id();
    }
}
