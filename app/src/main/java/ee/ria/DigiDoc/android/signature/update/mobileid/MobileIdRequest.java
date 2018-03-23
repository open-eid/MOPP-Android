package ee.ria.DigiDoc.android.signature.update.mobileid;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddRequest;

@AutoValue
public abstract class MobileIdRequest implements SignatureAddRequest {

    public abstract String phoneNo();

    public abstract String personalCode();

    public abstract boolean rememberMe();

    static MobileIdRequest create(String phoneNo, String personalCode, boolean rememberMe) {
        return new AutoValue_MobileIdRequest(phoneNo, personalCode, rememberMe);
    }
}
