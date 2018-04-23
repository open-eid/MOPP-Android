package ee.ria.DigiDoc.android.eid;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class CodeUpdateRequest {

    abstract String currentValue();

    abstract String newValue();

    abstract String repeatValue();

    static CodeUpdateRequest create(String currentValue, String newValue, String repeatValue) {
        return new AutoValue_CodeUpdateRequest(currentValue, newValue, repeatValue);
    }
}
