package ee.ria.DigiDoc.sign;

import android.content.Context;

import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public class SSLHandshakeException extends Exception implements SignatureUpdateError {

    public SSLHandshakeException() { }

    @Override
    public String getMessage(Context context) {
        return context.getString(getMessageId());
    }

    public int getMessageId() {
        return R.string.invalid_ssl_handshake;
    }
}
