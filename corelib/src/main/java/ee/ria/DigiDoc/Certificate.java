package ee.ria.DigiDoc;

import com.google.auto.value.AutoValue;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.cert.X509CertificateHolder;
import org.threeten.bp.Instant;

import java.io.IOException;

import okio.ByteString;

@AutoValue
public abstract class Certificate {

    public abstract EIDType type();

    public abstract String commonName();

    public abstract Instant notAfter();

    public abstract KeyUsage keyUsage();

    public abstract ExtendedKeyUsage extendedKeyUsage();

    public abstract ByteString data();

    public static Certificate create(ByteString data) throws IOException {
        X509CertificateHolder certificate = new X509CertificateHolder(data.toByteArray());
        Instant notAfter = Instant.ofEpochMilli(certificate.getNotAfter().getTime());

        RDN[] rdNs = certificate.getSubject().getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.O));
        String organization = rdNs[0].getFirst().getValue().toString().trim();

        rdNs = certificate.getSubject().getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.CN));
        String commonName = rdNs[0].getFirst().getValue().toString().trim();

        Extensions extensions = certificate.getExtensions();
        KeyUsage keyUsage = KeyUsage.fromExtensions(extensions);
        ExtendedKeyUsage extendedKeyUsage = ExtendedKeyUsage.fromExtensions(extensions);
        if (extendedKeyUsage == null) {
            extendedKeyUsage = new ExtendedKeyUsage(new KeyPurposeId[]{});
        }

        return new AutoValue_Certificate(EIDType.parseOrganization(organization), commonName,
                notAfter, keyUsage, extendedKeyUsage, data);
    }
}
