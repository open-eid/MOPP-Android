package ee.ria.DigiDoc.android.utils.files;

import android.content.Context;

import java.io.IOException;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public final class EmptyFileException extends IOException implements SignatureUpdateError {

    public EmptyFileException() {}

    @Override
    public String getMessage(Context context) {
        return context.getString(R.string.empty_file_error);
    }
}