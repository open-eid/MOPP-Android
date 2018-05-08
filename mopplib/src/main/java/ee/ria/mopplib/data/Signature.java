package ee.ria.mopplib.data;

import com.google.auto.value.AutoValue;

import org.threeten.bp.Instant;

@AutoValue
public abstract class Signature {

    /**
     * Unique ID per container.
     */
    public abstract String id();

    /**
     * Name to display.
     */
    public abstract String name();

    /**
     * Created date and time.
     */
    public abstract Instant createdAt();

    /**
     * Status of the signature.
     */
    @SignatureStatus public abstract String status();

    /**
     * Whether this signature is valid or invalid.
     *
     * Valid statuses:
     * {@link SignatureStatus#VALID}
     * {@link SignatureStatus#WARNING}
     * {@link SignatureStatus#NON_QSCD}
     *
     * Invalid statuses:
     * {@link SignatureStatus#INVALID}
     * {@link SignatureStatus#UNKNOWN}
     *
     * @return Validity of the signature.
     */
    public final boolean valid() {
        return !status().equals(SignatureStatus.INVALID)
                && !status().equals(SignatureStatus.UNKNOWN);
    }

    /**
     * Signature profile.
     */
    public abstract String profile();

    /**
     * Creates a new signature object.
     *
     * @param id Signature ID.
     * @param name Signature display name.
     * @param createdAt Signature created date and time.
     * @param status Signature status.
     * @param profile Signature profile.
     */
    public static Signature create(String id, String name, Instant createdAt,
                            @SignatureStatus String status, String profile) {
        return new AutoValue_Signature(id, name, createdAt, status, profile);
    }
}
