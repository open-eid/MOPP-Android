package ee.ria.DigiDoc.android.signature.update.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.mopplib.data.SignedContainer;

@AutoValue
public abstract class IdCardResponse implements SignatureAddResponse {

    public abstract boolean readerConnected();

    @Nullable public abstract IdCardData data();

    public abstract boolean signingActive();

    @Nullable public abstract Throwable error();

    @Nullable public abstract Byte retryCounter();

    @Override
    public boolean showDialog() {
        return true;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        return this;
    }

    public static IdCardResponse initial() {
        return create(null, false, false, null, false, null, null);
    }

    public static IdCardResponse reader() {
        return create(null, false, true, null, false, null, null);
    }

    public static IdCardResponse data(IdCardData data) {
        return create(null, false, true, data, false, null, null);
    }

    public static IdCardResponse signing() {
        return create(null, false, true, null, true, null, null);
    }

    public static IdCardResponse success(SignedContainer container) {
        return create(container, false, false, null, false, null, null);
    }

    public static IdCardResponse failure(IdCardData data, Throwable error, byte retryCounter) {
        return create(null, false, true, data, false, error, retryCounter);
    }

    private static IdCardResponse create(@Nullable SignedContainer container, boolean active,
                                         boolean readerConnected, @Nullable IdCardData data,
                                         boolean signingActive, @Nullable Throwable error,
                                         @Nullable Byte retryCounter) {
        return new AutoValue_IdCardResponse(container, active, readerConnected, data,
                signingActive, error, retryCounter);
    }
}
