package ee.ria.DigiDoc.crypto;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.openeid.cdoc4j.ECRecipient;
import org.openeid.cdoc4j.RSARecipient;
import org.openeid.cdoc4j.exception.DecryptionException;
import org.openeid.cdoc4j.token.Token;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

class PKCS11Token implements Token {

    private final DecryptToken token;
    private final X509Certificate certificate;
    private final String pin1;

    PKCS11Token(DecryptToken token, X509Certificate certificate, String pin1) {
        this.token = token;
        this.certificate = certificate;
        this.pin1 = pin1;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }

    @Override
    public byte[] decrypt(RSARecipient recipient) throws DecryptionException {
        try {
            return token.decrypt(pin1.getBytes(), recipient.getEncryptedKey(), false);
        } catch (Pin1InvalidException e) {
            throw new Pin1InvalidException.DecryptPin1InvalidException(e);
        } catch (CryptoException e) {
            throw new DecryptionException("Decrypting RSA recipient failed", e);
        }
    }

    @Override
    public byte[] decrypt(ECRecipient recipient) throws DecryptionException {
        SubjectPublicKeyInfo info = SubjectPublicKeyInfo
                .getInstance(recipient.getEphemeralPublicKey().getEncoded());
        try {
            return token.decrypt(pin1.getBytes(), info.getPublicKeyData().getBytes(), true);
        } catch (Pin1InvalidException e) {
            throw new Pin1InvalidException.DecryptPin1InvalidException(e);
        } catch (CryptoException e) {
            throw new DecryptionException("Decrypting EC recipient failed", e);
        }
    }
}
