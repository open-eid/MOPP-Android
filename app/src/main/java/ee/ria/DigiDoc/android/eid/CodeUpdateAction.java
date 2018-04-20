package ee.ria.DigiDoc.android.eid;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import ee.ria.tokenlibrary.Token;

@AutoValue
abstract class CodeUpdateAction implements Parcelable {

    abstract Token.PinType pinType();

    @CodeUpdateType abstract String updateType();

    static CodeUpdateAction create(Token.PinType pinType, @CodeUpdateType String updateType) {
        return new AutoValue_CodeUpdateAction(pinType, updateType);
    }
}
