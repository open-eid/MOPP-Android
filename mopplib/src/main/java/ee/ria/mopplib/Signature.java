package ee.ria.mopplib;

import com.google.auto.value.AutoValue;

import org.threeten.bp.Instant;

@AutoValue
public abstract class Signature {

    public abstract String id();

    public abstract String name();

    public abstract Instant createdAt();

    @SignatureStatus public abstract String status();
}
