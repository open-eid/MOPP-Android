package ee.ria.mopplib.data;

import android.support.annotation.StringDef;

import com.google.common.collect.ImmutableMap;

import static ee.ria.mopplib.data.SignatureStatus.INVALID;
import static ee.ria.mopplib.data.SignatureStatus.VALID;

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

    ImmutableMap<String, Integer> ORDER = ImmutableMap.<String, Integer>builder()
            .put(INVALID, 0)
            .put(VALID, 1)
            .build();
}
