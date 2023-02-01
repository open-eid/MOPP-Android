package ee.ria.DigiDoc.common.exception;

import java.io.IOException;

public class SigningCancelledException extends IOException {

    public SigningCancelledException(String message) {
        super(message);
    }
}
