package ee.ria.DigiDoc.android.signature.data;

import com.google.auto.value.AutoValue;

import org.threeten.bp.Instant;

@AutoValue
public abstract class Signature {

    public abstract String id();

    public abstract String name();

    public abstract Instant createdAt();

    public abstract boolean valid();

    public static Signature create(String id, String name, Instant createdAt, boolean valid) {
        return new AutoValue_Signature(id, name, createdAt, valid);
    }
}
