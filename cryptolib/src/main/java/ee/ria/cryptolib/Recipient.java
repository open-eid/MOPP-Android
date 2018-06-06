package ee.ria.cryptolib;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.Certificate;

@AutoValue
public abstract class Recipient {

    public abstract Certificate certificate();

    public abstract String name();

    public static Recipient create(Certificate certificate, String name) {
        return new AutoValue_Recipient(certificate, name);
    }
}
