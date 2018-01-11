package ee.ria.mopplib;

import android.support.annotation.StringDef;

import static ee.ria.mopplib.SignatureStatus.INVALID;
import static ee.ria.mopplib.SignatureStatus.VALID;

@StringDef({VALID, INVALID})
public @interface SignatureStatus {

    /**
     * Signature is valid.
     */
    String VALID = "VALID";

    /**
     * Signature is invalid.
     */
    String INVALID = "INVALID";
}
