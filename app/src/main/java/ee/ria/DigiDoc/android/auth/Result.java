package ee.ria.DigiDoc.android.auth;


import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.nio.ByteBuffer;

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;


public interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class AuthInitResult implements Result {

        @Nullable
        abstract IdCardDataResponse idCardDataResponse();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().authenticationIdCardDataResponse(idCardDataResponse()).build();
        }

        static AuthInitResult show(IdCardDataResponse idCardDataResponse) {
            return create(idCardDataResponse);
        }

        static AuthInitResult hide() {
            return create(null);
        }

        private static AuthInitResult create(@Nullable IdCardDataResponse idCardDataResponse) {
            return new AutoValue_Result_AuthInitResult(idCardDataResponse);
        }
    }

    @AutoValue
    abstract class AuthenticationResult implements Result {

        @Nullable
        abstract IdCardDataResponse idCardDataResponse();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().authenticationIdCardDataResponse(idCardDataResponse()).build();
        }

        static AuthenticationResult show(IdCardDataResponse idCardDataResponse) {
            return create(idCardDataResponse);
        }

        static AuthenticationResult hide() {
            return create(null);
        }

        private static AuthenticationResult create(@Nullable IdCardDataResponse idCardDataResponse) {
            return new AutoValue_Result_AuthenticationResult(idCardDataResponse);
        }
    }


    @AutoValue
    abstract class AuthActionResult implements Result {

        @State
        abstract String state();

        abstract boolean successMessageVisible();

        @Nullable
        abstract ByteBuffer signature();

        @Nullable
        abstract Throwable error();

        @Nullable
        abstract IdCardDataResponse idCardDataResponse();

        static AuthActionResult activity() {
            return create(State.ACTIVE, false, null, null, null);
        }

        static AuthActionResult successMessage(ByteBuffer signature) {
            return create(State.IDLE, true, signature, null, null);
        }

        static AuthActionResult failure(Throwable error,
                                        @Nullable IdCardDataResponse idCardDataResponse) {
            return create(State.IDLE, false, null, error, idCardDataResponse);
        }

        @Override
        public ViewState reduce(ViewState state) {

            IdCardDataResponse idCardDataResponse = idCardDataResponse();
            IdCardData idCardData = idCardDataResponse != null ? idCardDataResponse.data() : null;
            int pin1RetryCount = idCardData != null ? idCardData.pin1RetryCount() : -1;
            Throwable error = error();
            ViewState.Builder builder = state.buildWith()
                    .authenticationState(state())
                    .authenticationSuccessMessageVisible(successMessageVisible())
                    .authenticationError(error);
            if (signature() != null) {
                builder
                        .signature(signature())
                        .authenticationIdCardDataResponse(null)
                        .authenticationState(State.IDLE)
                        .authenticationError(null);
            }
            if (error != null && error instanceof Pin1InvalidException && pin1RetryCount > 0) {
                builder.authenticationIdCardDataResponse(idCardDataResponse);
            } else if (error != null) {
                builder.authenticationIdCardDataResponse(null);
            }
            return builder.build();
        }

        static AuthActionResult clear() {
            return create(State.CLEAR, false, null, null, null);
        }

        static AuthActionResult success(ByteBuffer signature) {
            return create(State.IDLE, false, signature, null, null);
        }
        static AuthActionResult show(IdCardDataResponse idCardDataResponse) {
            return create(State.IDLE, true, null, null, idCardDataResponse);
        }
        static AuthActionResult idle() {
            return create(State.IDLE, false, null, null, null);
        }
        private static AuthActionResult create(@State String state, boolean successMessageVisible,
                                               @Nullable ByteBuffer signature,
                                               @Nullable Throwable error,
                                               @Nullable IdCardDataResponse idCardDataResponse) {
            return new AutoValue_Result_AuthActionResult(state, successMessageVisible, signature,
                    error, idCardDataResponse);
        }
    }
}
