package ee.ria.EstEIDUtility.domain;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class X509Cert {

    private static final String TAG = "X509Cert";

    private X509Certificate certificate;

    public enum SubjectName {
        SERIALNUMBER,
        GIVENNAME,
        SURNAME
    }

    public X509Cert(byte[] signingCertificateDer) {
        certificate = getSignatureCertificate(signingCertificateDer);
    }

    public String getSubjectName(SubjectName part) {
        Map<String, String> subjectNamePartMap = loadSubjectNameParts();

        String subjectName = subjectNamePartMap.get(part.name());

        return subjectName;
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

    private Map<String, String> loadSubjectNameParts() {
        //TODO: the keys seem to be different in different android versions: probably different certificate providers?
        String[] parts = certificate.getSubjectX500Principal().toString().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        Map<String, String> partMap = new HashMap<>();
        for (String part : parts) {
            String[] strings = part.split("=(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String key = strings[0].trim();
            String value = strings[1].trim();
            partMap.put(key, value);
        }
        return partMap;
    }

    private X509Certificate getSignatureCertificate(byte[] signingCertificateDer) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(signingCertificateDer));
            return cert;
        } catch (CertificateException e) {
            Log.e(TAG, "CertificateFactory: ", e);
        }
        return null;
    }

}
