package ee.ria.DigiDoc.android.signature.update.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.mopplib.data.SignedContainer;

@AutoValue
public abstract class IdCardResponse implements SignatureAddResponse {

    public abstract boolean readerConnected();

    @Nullable public abstract IdCardData data();

    @Override
    public boolean showDialog() {
        return true;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        return this;
    }

    public static IdCardResponse initial() {
        return create(null, false, false, null);
    }

    public static IdCardResponse reader() {
        return create(null, false, true, null);
    }

    public static IdCardResponse data(IdCardData data) {
        return create(null, false, true, data);
    }

    private static IdCardResponse create(@Nullable SignedContainer container, boolean active,
                                         boolean readerConnected, @Nullable IdCardData data) {
        return new AutoValue_IdCardResponse(container, active, readerConnected, data);
    }
}
