package ee.ria.DigiDoc.configuration.verify;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import timber.log.Timber;

/**
 * Helper class for verifying configuration signature.
 */
class SignatureVerifier {

    static boolean verify(byte[] signature, String publicKeyPEM, String signedContent) {
        SubjectPublicKeyInfo publicKeyInfo = parsePublicKeyInfo(publicKeyPEM);
        if (publicKeyInfo != null) {
            PublicKey publicKey = convertPublicKeyInfoToPublicKey(publicKeyInfo);
            return verifySignature(signature, publicKey, signedContent);
        }
        return false;
    }

    private static PublicKey convertPublicKeyInfoToPublicKey(SubjectPublicKeyInfo publicKeyInfo) {
        try {
            RSAKeyParameters keyParams = (RSAKeyParameters) PublicKeyFactory.createKey(publicKeyInfo);
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(keyParams.getModulus(), keyParams.getExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
            Timber.e(e, "PublicKey conversion failed");
            throw new IllegalStateException("Failed to convert org.bouncycastle.asn1.x509.SubjectPublicKeyInfo to java.security.PublicKey", e);
        }
    }

    private static SubjectPublicKeyInfo parsePublicKeyInfo(String PKCS1PublicKeyPEM) {
        try (PEMParser pemParser = new PEMParser(new StringReader(PKCS1PublicKeyPEM))) {
            return (SubjectPublicKeyInfo) pemParser.readObject();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse PEM encoded PKCS#1 public key", e);
        }
    }

    private static boolean verifySignature(byte[] signatureBytes, PublicKey publicKey, String signedContent) {
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initVerify(publicKey);
            signature.update(signedContent.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            Timber.e(e, "Signature verification failed");
            throw new IllegalStateException("Failed to verify signature", e);
        }
    }
}
