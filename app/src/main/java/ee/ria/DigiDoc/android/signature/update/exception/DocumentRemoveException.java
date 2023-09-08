package ee.ria.DigiDoc.android.signature.update.exception;

import android.content.Context;

import java.io.IOException;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public final class DocumentRemoveException extends IOException implements SignatureUpdateError {

    public DocumentRemoveException() {}

    @Override
    public String getMessage(Context context) {
        return context.getString(R.string.signature_update_document_remove_error);
    }
}