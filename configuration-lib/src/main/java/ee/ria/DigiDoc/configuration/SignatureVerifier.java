package ee.ria.DigiDoc.configuration;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.encoders.Base64;

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

/**
 * Helper class for verifying configuration signature.
 */
public class SignatureVerifier {

    public static boolean verify(String base64EncodedSignature, String publicKeyPEM, String signedContent) {
        SubjectPublicKeyInfo publicKeyInfo = parsePublicKeyInfo(publicKeyPEM);
        PublicKey publicKey = convertPublicKeyInfoToPublicKey(publicKeyInfo);
        return verifySignature(base64EncodedSignature, publicKey, signedContent);
    }

    private static PublicKey convertPublicKeyInfoToPublicKey(SubjectPublicKeyInfo publicKeyInfo) {
        try {
            RSAKeyParameters keyParams = (RSAKeyParameters) PublicKeyFactory.createKey(publicKeyInfo);
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(keyParams.getModulus(), keyParams.getExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
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

    private static boolean verifySignature(String base64EncodedSignature, PublicKey publicKey, String signedContent) {
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initVerify(publicKey);
            signature.update(signedContent.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decode(base64EncodedSignature));
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to verify signature", e);
        }
    }
}
