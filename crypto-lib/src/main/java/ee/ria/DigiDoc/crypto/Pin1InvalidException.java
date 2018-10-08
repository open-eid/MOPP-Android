package ee.ria.DigiDoc.crypto;

import org.openeid.cdoc4j.exception.DecryptionException;

/**
 * Decryption failed because provided PIN1 is incorrect.
 */
public class Pin1InvalidException extends CryptoException {

    /**
     * Used to identify PIN1-invalid-case from token.
     */
    static final class DecryptPin1InvalidException extends DecryptionException {

        DecryptPin1InvalidException(Exception e) {
            super("PIN1 is invalid", e);
        }
    }
}
