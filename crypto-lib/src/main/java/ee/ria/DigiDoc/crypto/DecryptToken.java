package ee.ria.DigiDoc.crypto;

import java.io.File;

import ee.ria.DigiDoc.core.Certificate;

/**
 * Handles the actual decryption.
 *
 * Has to be provided when calling
 * {@link CryptoContainer#decrypt(DecryptToken, Certificate, String, File)}.
 */
public interface DecryptToken {

    /**
     * Perform the actual decryption.
     *
     * @param pin1 PIN1 code.
     * @param data Data to sign.
     * @param ecc Whether it is an elliptic curve certificate.
     * @return Decrypted data.
     * @throws Pin1InvalidException When PIN1 verification fails.
     * @throws CryptoException When something besides PIN1 verification fails.
     */
    byte[] decrypt(byte[] pin1, byte[] data, boolean ecc) throws CryptoException;
}
