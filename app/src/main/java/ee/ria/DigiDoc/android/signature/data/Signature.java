package ee.ria.DigiDoc.android.signature.data;

import com.google.auto.value.AutoValue;

import org.threeten.bp.Instant;

@AutoValue
public abstract class Signature {

    public abstract String name();

    public abstract Instant createdAt();

    public abstract boolean valid();

    public static Signature create(String name, Instant createdAt, boolean valid) {
        return new AutoValue_Signature(name, createdAt, valid);
    }
}
