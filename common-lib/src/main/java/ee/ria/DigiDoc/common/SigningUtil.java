package ee.ria.DigiDoc.common;

import ee.ria.DigiDoc.common.exception.SigningCancelledException;

public class SigningUtil {

    public static void checkSigningCancelled(boolean isCancelled) throws SigningCancelledException {
        if (isCancelled) {
            throw new SigningCancelledException("Signing cancelled");
        }
    }
}
