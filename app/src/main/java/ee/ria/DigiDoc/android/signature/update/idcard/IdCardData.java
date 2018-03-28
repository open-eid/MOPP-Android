package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class IdCardData {

    abstract String personalCode();

    abstract String name();

    static IdCardData create(String personalCode, String name) {
        return new AutoValue_IdCardData(personalCode, name);
    }
}
