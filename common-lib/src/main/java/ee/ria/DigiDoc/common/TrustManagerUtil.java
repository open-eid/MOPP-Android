package ee.ria.DigiDoc.common;

import com.unboundid.util.ssl.SSLUtil;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustManagerUtil {

    public static SSLUtil createDefaultKeystoreSSLUtil() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        return new SSLUtil(new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                checkTrustManagerCertificates(trustManagers, CERT_CHECK.CHECK_CLIENT, x509Certificates, s);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                checkTrustManagerCertificates(trustManagers, CERT_CHECK.CHECK_SERVER, x509Certificates, s);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return getCertificateAcceptedIssuers(trustManagers);
            }
        });
    }

    private enum CERT_CHECK {
        CHECK_CLIENT,
        CHECK_SERVER
    }

    private static X509Certificate[] getCertificateAcceptedIssuers(TrustManager[] trustManagers) {
        for (TrustManager trustManager: trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return ((X509TrustManager) trustManager).getAcceptedIssuers();
            }
        }

        return new X509Certificate[]{};
    }

    private static void checkTrustManagerCertificates(TrustManager[] trustManagers,
                                                      CERT_CHECK certCheck, X509Certificate[] x509Certificates,
                                                      String s) throws CertificateException {
        for (TrustManager trustManager: trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                switch (certCheck) {
                    case CHECK_CLIENT:
                        ((X509TrustManager) trustManager).checkClientTrusted(x509Certificates, s);
                        break;
                    case CHECK_SERVER:
                        ((X509TrustManager) trustManager).checkServerTrusted(x509Certificates, s);
                        break;
                }
            }
        }
    }
}
