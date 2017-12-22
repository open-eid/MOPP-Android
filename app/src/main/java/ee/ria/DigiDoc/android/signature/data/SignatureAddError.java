package ee.ria.DigiDoc.android.signature.data;

import android.support.annotation.StringRes;

public final class SignatureAddError extends Exception {

    private final int messageRes;

    public SignatureAddError(@StringRes int messageRes) {
        this.messageRes = messageRes;
    }

    @StringRes public int getMessageRes() {
        return messageRes;
    }
}
