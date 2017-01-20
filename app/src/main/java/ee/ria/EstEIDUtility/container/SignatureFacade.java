package ee.ria.EstEIDUtility.container;

import ee.ria.libdigidocpp.Signature;

public class SignatureFacade {

    private final Signature signature;

    SignatureFacade(Signature signature) {
        this.signature = signature;
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
            return false;
        }
    }

}
