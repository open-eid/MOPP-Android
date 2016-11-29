package ee.ria.token.tokenservice.callback;

public interface SignCallback {
    void onSignResponse(byte[] signature);
    void onSignError(String msg);
}
