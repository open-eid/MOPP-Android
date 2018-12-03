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
    UNKNOWN,

    /**
     * Test signature, only applicable when in test environment.
     */
    TEST;

    public static final ImmutableMap<SignatureStatus, Integer> ORDER =
            ImmutableMap.<SignatureStatus, Integer>builder()
                    .put(TEST, 0)
                    .put(UNKNOWN, 1)
                    .put(INVALID, 2)
                    .put(NON_QSCD, 3)
                    .put(WARNING, 4)
                    .put(VALID, 5)
                    .build();
}
