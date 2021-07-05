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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import okio.ByteString;

/**
 * Wrapper class for all the necessary things from a certificate the application needs.
 */
@AutoValue
public abstract class Certificate {

    public abstract EIDType type();

    public abstract String commonName();

    public abstract String friendlyName();

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

        RDN[] rdSNNs = certificate.getSubject().getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.SURNAME));
        RDN[] rdGNNs = certificate.getSubject().getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.GIVENNAME));
        RDN[] rdSERIALNs = certificate.getSubject().getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.SERIALNUMBER));

        // http://www.etsi.org/deliver/etsi_en/319400_319499/31941201/01.01.01_60/en_31941201v010101p.pdf
        final List<String> types = Arrays.asList("PAS", "IDC", "PNO", "TAX", "TIN");
        String serialNR = rdSERIALNs.length == 0 ? "" : rdSERIALNs[0].getFirst().getValue().toString().trim();
        if(serialNR.length() > 6 && (types.contains(serialNR.substring(0, 3)) || serialNR.charAt(2) == ':') && serialNR.charAt(5) == '-')
            serialNR = serialNR.substring(6);

        String friendlyName = rdSNNs.length == 0 || rdGNNs.length == 0 ? commonName :
                rdSNNs[0].getFirst().getValue().toString().trim() + "," +
                rdGNNs[0].getFirst().getValue().toString().trim() + "," + serialNR;

        Instant notAfter = Instant.ofEpochMilli(certificate.getNotAfter().getTime());

        boolean ellipticCurve = certificate.getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm()
                .equals(X9ObjectIdentifiers.id_ecPublicKey);

        KeyUsage keyUsage = KeyUsage.fromExtensions(extensions);

        ExtendedKeyUsage extendedKeyUsage = ExtendedKeyUsage.fromExtensions(extensions);
        if (extendedKeyUsage == null) {
            extendedKeyUsage = new ExtendedKeyUsage(new KeyPurposeId[]{});
        }

        return new AutoValue_Certificate(type, commonName, friendlyName, notAfter, ellipticCurve, keyUsage,
                extendedKeyUsage, data);
    }
}
