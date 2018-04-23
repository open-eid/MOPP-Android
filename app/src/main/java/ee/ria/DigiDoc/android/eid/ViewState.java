package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract IdCardDataResponse idCardDataResponse();

    @Nullable abstract Throwable error();

    abstract boolean certificatesContainerExpanded();

    @Nullable abstract CodeUpdateAction codeUpdateAction();

    @Nullable abstract CodeUpdateResponse codeUpdateResponse();

    abstract boolean codeUpdateActivity();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .idCardDataResponse(IdCardDataResponse.initial())
                .certificatesContainerExpanded(false)
                .codeUpdateActivity(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder idCardDataResponse(IdCardDataResponse idCardDataResponse);
        Builder error(@Nullable Throwable error);
        Builder certificatesContainerExpanded(boolean certificatesContainerExpanded);
        Builder codeUpdateAction(@Nullable CodeUpdateAction codeUpdateAction);
        Builder codeUpdateResponse(@Nullable CodeUpdateResponse codeUpdateResponse);
        Builder codeUpdateActivity(boolean codeUpdateActivity);
        ViewState build();
    }
}
