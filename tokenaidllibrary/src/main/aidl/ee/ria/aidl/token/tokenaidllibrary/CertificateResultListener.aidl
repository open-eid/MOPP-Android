// CertificateResultListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface CertificateResultListener {
    oneway void onCertifiacteRequestSuccess(String certificateInHex);
    oneway void onCertifiacteRequestFailed(String reason);
}
