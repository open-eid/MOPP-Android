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
     * Creates a new signature object.
     *
     * Should only be accessed from {@link SignedContainer}.
     *
     * @param id Signature ID.
     * @param name Signature display name.
     * @param createdAt Signature created date and time.
     * @param status Signature status.
     */
    static Signature create(String id, String name, Instant createdAt,
                            @SignatureStatus String status) {
        return new AutoValue_Signature(id, name, createdAt, status);
    }
}
