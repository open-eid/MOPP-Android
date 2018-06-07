package ee.ria.DigiDoc.android.model;

import com.google.auto.value.AutoValue;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.threeten.bp.Instant;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import okio.ByteString;

@AutoValue
public abstract class CertificateData {

    public abstract int pinRetryCount();

    public abstract ByteString data();

    public abstract boolean ellipticCurve();

    public abstract Instant notAfter();

    public abstract String organization();

    public boolean expired() {
        return notAfter().isBefore(Instant.now());
    }

    public static CertificateData create(int pinRetryCount, ByteString data) throws
            CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(data.toByteArray()));
        boolean ellipticCurve = certificate.getPublicKey() instanceof ECPublicKey;
        Instant notAfter = Instant.ofEpochMilli(certificate.getNotAfter().getTime());
        X500Name x500name = new JcaX509CertificateHolder(certificate).getSubject();
        RDN[] rdNs = x500name.getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.O));
        String organization = rdNs[0].getFirst().getValue().toString().trim();
        return new AutoValue_CertificateData(pinRetryCount, data, ellipticCurve, notAfter,
                organization);
    }
}
