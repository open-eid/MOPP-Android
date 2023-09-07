package ee.ria.DigiDoc.sign;

import android.content.Context;
import android.text.Spanned;

import ee.ria.DigiDoc.common.exception.SignatureUpdateDetailError;

public class OcspInvalidTimeSlotException extends Exception implements SignatureUpdateDetailError {

    private Spanned detailMessage;
    private String message;

    public OcspInvalidTimeSlotException() {}

    public OcspInvalidTimeSlotException(Spanned detailMessage) {
        this.detailMessage = detailMessage;
    }

    public OcspInvalidTimeSlotException(String message) {
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
