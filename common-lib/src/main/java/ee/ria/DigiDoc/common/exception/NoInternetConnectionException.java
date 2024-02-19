package ee.ria.DigiDoc.common.exception;

import android.content.Context;

import ee.ria.DigiDoc.common.R;

public class NoInternetConnectionException extends Exception implements SignatureUpdateError {

    public NoInternetConnectionException() { }

    @Override
    public String getMessage(Context context) {
        return context.getString(R.string.no_internet_connection);
    }
}
