package ee.ria.DigiDoc.crypto;

/**
 * General exception type for crypto library.
 */
public class CryptoException extends Exception {

    CryptoException() {
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
