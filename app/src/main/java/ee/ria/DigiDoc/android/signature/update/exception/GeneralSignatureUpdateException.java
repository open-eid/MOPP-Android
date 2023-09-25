package ee.ria.DigiDoc.android.signature.update.exception;

import android.content.Context;

import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public class GeneralSignatureUpdateException extends Exception implements SignatureUpdateError {

    private final String message;

    public GeneralSignatureUpdateException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage(Context context) {
        return message;
    }
}
