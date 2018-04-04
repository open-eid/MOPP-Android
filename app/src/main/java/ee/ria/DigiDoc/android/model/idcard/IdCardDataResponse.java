package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class IdCardDataResponse {

    @IdCardStatus public abstract String status();

    @Nullable public abstract IdCardData data();

    public static IdCardDataResponse initial() {
        return create(IdCardStatus.INITIAL, null);
    }

    public static IdCardDataResponse readerDetected() {
        return create(IdCardStatus.READER_DETECTED, null);
    }

    public static IdCardDataResponse cardDetected() {
        return create(IdCardStatus.CARD_DETECTED, null);
    }

    public static IdCardDataResponse data(IdCardData data) {
        return create(IdCardStatus.CARD_DETECTED, data);
    }

    private static IdCardDataResponse create(@IdCardStatus String status,
                                             @Nullable IdCardData data) {
        return new AutoValue_IdCardDataResponse(status, data);
    }
}
