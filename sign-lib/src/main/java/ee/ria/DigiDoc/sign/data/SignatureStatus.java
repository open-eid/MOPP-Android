package ee.ria.DigiDoc.sign.data;

import android.support.annotation.StringDef;

import com.google.common.collect.ImmutableMap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static ee.ria.DigiDoc.sign.data.SignatureStatus.INVALID;
import static ee.ria.DigiDoc.sign.data.SignatureStatus.NON_QSCD;
import static ee.ria.DigiDoc.sign.data.SignatureStatus.UNKNOWN;
import static ee.ria.DigiDoc.sign.data.SignatureStatus.VALID;
import static ee.ria.DigiDoc.sign.data.SignatureStatus.WARNING;

@StringDef({VALID, WARNING, NON_QSCD, INVALID, UNKNOWN})
@Retention(RetentionPolicy.SOURCE)
public @interface SignatureStatus {

    /**
     * Signature is valid.
     */
    String VALID = "VALID";

    /**
     * Signature is valid with warnings.
     */
    String WARNING = "WARNING";

    /**
     * Signature is valid with restrictions.
     */
    String NON_QSCD = "NON_QSCD";

    /**
     * Signature is invalid.
     */
    String INVALID = "INVALID";

    /**
     * Signature status is unknown.
     */
    String UNKNOWN = "UNKNOWN";

    ImmutableMap<String, Integer> ORDER = ImmutableMap.<String, Integer>builder()
            .put(UNKNOWN, 0)
            .put(INVALID, 1)
            .put(NON_QSCD, 2)
            .put(WARNING, 3)
            .put(VALID, 4)
            .build();
}
