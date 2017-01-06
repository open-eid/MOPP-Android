package ee.ria.EstEIDUtility.domain;

import android.util.Log;

import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.IETFUtils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class X509Cert {

    private static final String TAG = "X509Cert";

    private X509Certificate certificate;

    public X509Cert(byte[] signingCertificateDer) {
        certificate = getSignatureCertificate(signingCertificateDer);
    }

    public boolean isValid() {
        try {
            certificate.checkValidity(new Date());
        } catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        }
        return true;
    }

    public String getValueByObjectIdentifier(ASN1ObjectIdentifier identifier) {
        X500Name x500name = null;
        try {
            x500name = new JcaX509CertificateHolder(certificate).getSubject();
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "getValueByObjectIdentifier: ", e);
        }
        if (x500name == null) {
            return null;
        }
        RDN[] rdNs = x500name.getRDNs(identifier);
        if (rdNs.length == 0) {
            return null;
        }
        RDN c = rdNs[0];
        return IETFUtils.valueToString(c.getFirst().getValue());
    }


    private X509Certificate getSignatureCertificate(byte[] signingCertificateDer) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(signingCertificateDer));
        } catch (CertificateException e) {
            Log.e(TAG, "CertificateFactory: ", e);
        }
        return null;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}
