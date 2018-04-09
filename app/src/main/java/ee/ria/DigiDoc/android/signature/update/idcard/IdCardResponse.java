package ee.ria.DigiDoc.android.signature.update.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.mopplib.data.SignedContainer;

@AutoValue
public abstract class IdCardResponse implements SignatureAddResponse {

    @Nullable public abstract IdCardDataResponse dataResponse();

    public abstract boolean signingActive();

    @Nullable public abstract Throwable error();

    @Nullable public abstract Byte retryCounter();

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
        return create(null, dataResponse, false, null, null);
    }

    public static IdCardResponse signing() {
        return create(null, null, true, null, null);
    }

    public static IdCardResponse success(SignedContainer container) {
        return create(container, null, false, null, null);
    }

    public static IdCardResponse failure(Throwable error, byte retryCounter) {
        return create(null, null, false, error, retryCounter);
    }

    private static IdCardResponse create(@Nullable SignedContainer container,
                                         @Nullable IdCardDataResponse dataResponse,
                                         boolean signingActive, @Nullable Throwable error,
                                         @Nullable Byte retryCounter) {
        return new AutoValue_IdCardResponse(container, dataResponse, signingActive, error,
                retryCounter);
    }
}
