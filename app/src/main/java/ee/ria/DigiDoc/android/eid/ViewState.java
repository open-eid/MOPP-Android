package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.IdCardData;
import ee.ria.DigiDoc.android.model.IdCardStatus;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    @IdCardStatus abstract String idCardStatus();

    @Nullable abstract IdCardData idCardData();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .idCardStatus(IdCardStatus.INITIAL)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder idCardStatus(@IdCardStatus String idCardStatus);
        Builder idCardData(@Nullable IdCardData idCardData);
        ViewState build();
    }
}
