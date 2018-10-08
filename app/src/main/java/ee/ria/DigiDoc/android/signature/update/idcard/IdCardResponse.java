package ee.ria.DigiDoc.android.signature.update.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardSignResponse;
import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.DigiDoc.sign.SignedContainer;

@AutoValue
public abstract class IdCardResponse implements SignatureAddResponse {

    @Nullable public abstract IdCardDataResponse dataResponse();

    @Nullable public abstract IdCardSignResponse signResponse();

    @Override
    public boolean active() {
        return false;
    }

    @Override
    public boolean showDialog() {
        return true;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        return this;
    }

    public static IdCardResponse initial() {
        return data(IdCardDataResponse.initial());
    }

    public static IdCardResponse data(IdCardDataResponse dataResponse) {
        return create(null, dataResponse, null);
    }

    public static IdCardResponse sign(IdCardSignResponse signResponse) {
        return create(null, null, signResponse);
    }

    public static IdCardResponse success(SignedContainer container) {
        return create(container, null, null);
    }

    private static IdCardResponse create(@Nullable SignedContainer container,
                                         @Nullable IdCardDataResponse dataResponse,
                                         @Nullable IdCardSignResponse signResponse) {
        return new AutoValue_IdCardResponse(container, dataResponse, signResponse);
    }
}
