package ee.ria.DigiDoc.common.exception;

import android.content.Context;

import ee.ria.DigiDoc.common.R;

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
