package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class IdCardDataResponse {

    @IdCardStatus public abstract String status();

    @Nullable public abstract IdCardData data();

    @Nullable public abstract Throwable error();

    public static IdCardDataResponse initial() {
        return create(IdCardStatus.INITIAL, null, null);
    }

    public static IdCardDataResponse readerDetected() {
        return create(IdCardStatus.READER_DETECTED, null, null);
    }

    public static IdCardDataResponse cardDetected() {
        return create(IdCardStatus.CARD_DETECTED, null, null);
    }

    public static IdCardDataResponse success(IdCardData data) {
        return create(IdCardStatus.CARD_DETECTED, data, null);
    }

    public static IdCardDataResponse failure(Throwable error) {
        return create(IdCardStatus.CARD_DETECTED, null, error);
    }

    private static IdCardDataResponse create(@IdCardStatus String status,
                                             @Nullable IdCardData data, @Nullable Throwable error) {
        return new AutoValue_IdCardDataResponse(status, data, error);
    }
}
