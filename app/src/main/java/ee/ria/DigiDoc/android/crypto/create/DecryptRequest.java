package ee.ria.DigiDoc.android.crypto.create;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.tokenlibrary.Token;

@AutoValue
abstract class DecryptRequest {

    abstract Token token();

    abstract File containerFile();

    abstract String pin1();

    static DecryptRequest create(Token token, File containerFile, String pin1) {
        return new AutoValue_DecryptRequest(token, containerFile, pin1);
    }
}
