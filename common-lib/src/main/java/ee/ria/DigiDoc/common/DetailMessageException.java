package ee.ria.DigiDoc.common;

import android.content.Context;
import android.text.Spanned;

import ee.ria.DigiDoc.common.exception.SignatureUpdateDetailError;
import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public class DetailMessageException implements SignatureUpdateError, SignatureUpdateDetailError {

    private Spanned detailMessage;
    private String message;

    public DetailMessageException(Spanned detailMessage) {
        this.detailMessage = detailMessage;
    }

    public DetailMessageException(String message) {
        this.message = message;
    }

    @Override
    public Spanned getDetailMessage(Context context) {
        return detailMessage;
    }

    @Override
    public String getMessage(Context context) {
        return message;
    }
}
