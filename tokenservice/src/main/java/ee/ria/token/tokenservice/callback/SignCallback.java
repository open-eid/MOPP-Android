package ee.ria.token.tokenservice.callback;

import ee.ria.token.tokenservice.exception.PinVerificationException;

public interface SignCallback {
    void onSignResponse(byte[] signature);
    void onSignError(Exception e, PinVerificationException pinVerificationException);
}
