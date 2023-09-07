package ee.ria.DigiDoc.sign;

import android.content.Context;

import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public class NoInternetConnectionException extends Exception implements SignatureUpdateError {

    public NoInternetConnectionException() { }

    @Override
    public String getMessage(Context context) {
        return context.getString(R.string.no_internet_connection);
    }
}
