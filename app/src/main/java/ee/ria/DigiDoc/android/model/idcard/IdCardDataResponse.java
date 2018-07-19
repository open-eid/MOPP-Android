package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.idcard.Token;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderStatus;

@AutoValue
public abstract class IdCardDataResponse {

    @SmartCardReaderStatus public abstract String status();

    @Nullable public abstract IdCardData data();

    @Nullable public abstract Throwable error();

    @Nullable public abstract Token token();

    public static IdCardDataResponse initial() {
        return create(SmartCardReaderStatus.IDLE, null, null, null);
    }

    public static IdCardDataResponse readerDetected() {
        return create(SmartCardReaderStatus.READER_DETECTED, null, null, null);
    }

    public static IdCardDataResponse cardDetected() {
        return create(SmartCardReaderStatus.CARD_DETECTED, null, null, null);
    }

    public static IdCardDataResponse success(IdCardData data, Token token) {
        return create(SmartCardReaderStatus.CARD_DETECTED, data, null, token);
    }

    public static IdCardDataResponse failure(Throwable error) {
        return create(SmartCardReaderStatus.CARD_DETECTED, null, error, null);
    }

    private static IdCardDataResponse create(@SmartCardReaderStatus String status,
                                             @Nullable IdCardData data, @Nullable Throwable error,
                                             @Nullable Token token) {
        return new AutoValue_IdCardDataResponse(status, data, error, token);
    }
}
