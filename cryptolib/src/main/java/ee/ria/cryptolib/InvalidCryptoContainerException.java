package ee.ria.cryptolib;

/**
 * Crypto container is invalid.
 *
 * This means that the container either could not be opened or parsed.
 */
public final class InvalidCryptoContainerException extends Exception {

    public InvalidCryptoContainerException(Throwable cause) {
        super(cause);
    }
}
