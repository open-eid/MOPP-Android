package ee.ria.cryptolib;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;

@AutoValue
public abstract class Recipient {

    public abstract String name();

    public abstract String type();

    public abstract LocalDate expiryDate();

    public static Recipient create(String name, String type, LocalDate expiryDate) {
        return new AutoValue_Recipient(name, type, expiryDate);
    }
}
