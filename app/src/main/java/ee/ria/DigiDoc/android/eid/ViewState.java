package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract IdCardDataResponse idCardDataResponse();

    @Nullable abstract Throwable error();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .idCardDataResponse(IdCardDataResponse.initial())
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder idCardDataResponse(IdCardDataResponse idCardDataResponse);
        Builder error(@Nullable Throwable error);
        ViewState build();
    }
}
