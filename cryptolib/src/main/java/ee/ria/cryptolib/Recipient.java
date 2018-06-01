package ee.ria.cryptolib;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;

import ee.ria.DigiDoc.EIDType;

@AutoValue
public abstract class Recipient {

    @Nullable public abstract EIDType type();

    public abstract String name();

    public abstract LocalDate expiryDate();

    public static Recipient create(@Nullable EIDType type, String name, LocalDate expiryDate) {
        return new AutoValue_Recipient(type, name, expiryDate);
    }
}
