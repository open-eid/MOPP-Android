package ee.ria.EstEIDUtility.container;

import ee.ria.libdigidocpp.Signature;
import timber.log.Timber;

public class SignatureFacade {

    private static final String TAG = SignatureFacade.class.getName();
    private final Signature signature;

    SignatureFacade(Signature signature) {
        this.signature = signature;
        Timber.tag(TAG);
    }

    /**
     * If container has signatures, you must use the same profile
     * @param profile can be 'time-stamp' or 'time-mark'
     */
    public void extendSignatureProfile(String profile) {
        signature.extendSignatureProfile(profile);
    }

    public byte[] getSigningCertificateDer() {
        return signature.signingCertificateDer();
    }

    public String getTrustedSigningTime() {
        return signature.trustedSigningTime();
    }

    public boolean isSignatureValid() {
        try {
            signature.validate();
            return true;
        } catch (Exception e) {
            Timber.e(e, "Signature validation failed");
            return false;
        }
    }

    public String getId() {
        return signature.id();
    }
}
