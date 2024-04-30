package ee.ria.DigiDoc.android.signature.update.nfc;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddRequest;

@AutoValue
public abstract class NFCRequest implements SignatureAddRequest {

    public abstract String can();

    public abstract String pin2();

    public static NFCRequest create(String can, String pin2) {
        return new AutoValue_NFCRequest(can, pin2);
    }
}
