package ee.ria.DigiDoc.android.model;

import com.google.auto.value.AutoValue;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import okio.ByteString;

@AutoValue
public abstract class CertificateData {

    public abstract ByteString data();

    public abstract boolean ellipticCurve();

    public abstract String organization();

    public static CertificateData create(ByteString data) throws CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(data.toByteArray()));
        boolean ellipticCurve = certificate.getPublicKey() instanceof ECPublicKey;
        X500Name x500name = new JcaX509CertificateHolder(certificate).getSubject();
        RDN[] rdNs = x500name.getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.O));
        String organization = rdNs[0].getFirst().getValue().toString().trim();
        return new AutoValue_CertificateData(data, ellipticCurve, organization);
    }
}
