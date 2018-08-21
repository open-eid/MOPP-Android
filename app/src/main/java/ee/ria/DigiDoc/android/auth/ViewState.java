package ee.ria.DigiDoc.android.auth;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.nio.ByteBuffer;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.BundleUtils;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;


@AutoValue
abstract class ViewState implements MviViewState {

    @Nullable
    abstract String hash();
    @Nullable abstract IdCardDataResponse authenticationIdCardDataResponse();
    @Nullable abstract ByteBuffer signature();
    @State
    abstract String authenticationState();
    abstract boolean authenticationSuccessMessageVisible();
    @Nullable abstract Throwable authenticationError();
    abstract ViewState.Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .authenticationState(State.IDLE)
                .authenticationSuccessMessageVisible(false)
                .build();

    }

    @AutoValue.Builder
    interface Builder {
        Builder hash(@Nullable String hash);
        Builder signature(ByteBuffer signature);
        Builder authenticationIdCardDataResponse(
                @Nullable IdCardDataResponse authenticationIdCardDataResponse);
        Builder authenticationState(@State String authenticationState);
        Builder authenticationSuccessMessageVisible(boolean decryptSuccessMessageVisible);
        Builder authenticationError(@Nullable Throwable authenticationError);
        ViewState build();
    }
}
