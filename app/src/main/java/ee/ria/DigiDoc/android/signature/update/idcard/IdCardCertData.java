package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import ee.ria.tokenlibrary.Token;
import okio.ByteString;

@AutoValue
public abstract class IdCardCertData {

    public abstract Token.CertType type();

    public abstract ByteString data();

    public abstract boolean ellipticCurve();

    static IdCardCertData create(Token.CertType type, ByteString data) throws CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(data.toByteArray()));
        return new AutoValue_IdCardCertData(type, data,
                certificate.getPublicKey() instanceof ECPublicKey);
    }
}
