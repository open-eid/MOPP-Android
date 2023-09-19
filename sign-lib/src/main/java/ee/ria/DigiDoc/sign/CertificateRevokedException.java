package ee.ria.DigiDoc.sign;

import android.content.Context;

import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public class CertificateRevokedException extends Exception implements SignatureUpdateError {

    private String message;

    CertificateRevokedException() {}

    public CertificateRevokedException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage(Context context) {
        return message;
    }
}
