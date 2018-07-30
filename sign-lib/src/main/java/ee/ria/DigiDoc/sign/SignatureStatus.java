package ee.ria.DigiDoc.sign;

import com.google.common.collect.ImmutableMap;

public enum SignatureStatus {

    /**
     * Signature is valid.
     */
    VALID,

    /**
     * Signature is valid with warnings.
     */
    WARNING,

    /**
     * Signature is valid with restrictions.
     */
    NON_QSCD,

    /**
     * Signature is invalid.
     */
    INVALID,

    /**
     * Signature status is unknown.
     */
    UNKNOWN;

    public static final ImmutableMap<SignatureStatus, Integer> ORDER =
            ImmutableMap.<SignatureStatus, Integer>builder()
                    .put(UNKNOWN, 0)
                    .put(INVALID, 1)
                    .put(NON_QSCD, 2)
                    .put(WARNING, 3)
                    .put(VALID, 4)
                    .build();
}
