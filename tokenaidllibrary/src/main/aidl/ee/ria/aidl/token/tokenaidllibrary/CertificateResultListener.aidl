// CertificateResultListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface CertificateResultListener {
    void onCertifiacteRequestSuccess(String certificateInHex);
    void onCertifiacteRequestFailed(String reason);
}
