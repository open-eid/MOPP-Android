package ee.ria.DigiDoc.common;

import com.google.auto.value.AutoValue;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.threeten.bp.Instant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import okio.ByteString;

/**
 * Wrapper class for all the necessary things from a certificate the application needs.
 */
@AutoValue
public abstract class Certificate {

    public abstract EIDType type();

    public abstract String commonName();

    public abstract Instant notAfter();

    public abstract boolean ellipticCurve();

    public abstract KeyUsage keyUsage();

    public abstract ExtendedKeyUsage extendedKeyUsage();

    public abstract ByteString data();

    public boolean expired() {
        return notAfter().isBefore(Instant.now());
    }

    public X509Certificate x509Certificate() throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(data().toByteArray()));
    }

    public static Certificate create(ByteString data) throws IOException {
        X509CertificateHolder certificate = new X509CertificateHolder(data.toByteArray());
        Extensions extensions = certificate.getExtensions();

        CertificatePolicies certificatePolicies = CertificatePolicies.fromExtensions(extensions);
        EIDType type = EIDType.parse(certificatePolicies);

        RDN[] rdNs = certificate.getSubject().getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.CN));
        String commonName = rdNs[0].getFirst().getValue().toString().trim();

        Instant notAfter = Instant.ofEpochMilli(certificate.getNotAfter().getTime());

        boolean ellipticCurve = certificate.getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm()
                .equals(X9ObjectIdentifiers.id_ecPublicKey);

        KeyUsage keyUsage = KeyUsage.fromExtensions(extensions);

        ExtendedKeyUsage extendedKeyUsage = ExtendedKeyUsage.fromExtensions(extensions);
        if (extendedKeyUsage == null) {
            extendedKeyUsage = new ExtendedKeyUsage(new KeyPurposeId[]{});
        }

        return new AutoValue_Certificate(type, commonName, notAfter, ellipticCurve, keyUsage,
                extendedKeyUsage, data);
    }
}
