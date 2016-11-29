package ee.ria.token.tokenservice.callback;

public interface CertCallback {

    void onCertificateResponse(byte[] cert);
    void onCertificateError(String msg);

}
