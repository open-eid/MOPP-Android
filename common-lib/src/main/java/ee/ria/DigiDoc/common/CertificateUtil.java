package ee.ria.DigiDoc.common;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class CertificateUtil {

    private CertificateUtil() {
    }

    public static X509Certificate x509Certificate(String certPem) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
    }

    public static X509Certificate x509Certificate(byte[] certDer) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certDer));
    }

}
