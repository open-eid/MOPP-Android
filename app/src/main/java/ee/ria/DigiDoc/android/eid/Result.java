package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardStatus;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.tokenlibrary.Token;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class LoadResult implements Result {

        @Nullable abstract IdCardDataResponse idCardDataResponse();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            IdCardDataResponse idCardDataResponse = idCardDataResponse();

            ViewState.Builder builder = state.buildWith();

            if (idCardDataResponse == null && error() == null) {
                builder
                        .idCardDataResponse(IdCardDataResponse.initial())
                        .error(null)
                        .codeUpdateAction(null);
            } else if (idCardDataResponse != null) {
                builder.idCardDataResponse(idCardDataResponse);
                if (!idCardDataResponse.status().equals(IdCardStatus.CARD_DETECTED)) {
                    builder.codeUpdateAction(null);
                }
            } else if (error() != null) {
                builder.error(error()).codeUpdateAction(null);
            }
            return builder.build();
        }

        static LoadResult success(IdCardDataResponse idCardDataResponse) {
            return create(idCardDataResponse, null);
        }

        static LoadResult failure(Throwable error) {
            return create(null, error);
        }

        static LoadResult clear() {
            return create(null, null);
        }

        private static LoadResult create(@Nullable IdCardDataResponse idCardDataResponse,
                                         @Nullable Throwable error) {
            return new AutoValue_Result_LoadResult(idCardDataResponse, error);
        }
    }

    @AutoValue
    abstract class CertificatesTitleClickResult implements Result {

        abstract boolean expanded();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .certificatesContainerExpanded(expanded())
                    .build();
        }

        static CertificatesTitleClickResult create(boolean expanded) {
            return new AutoValue_Result_CertificatesTitleClickResult(expanded);
        }
    }

    @AutoValue
    abstract class CodeUpdateResult implements Result {

        @State abstract String state();

        @Nullable abstract CodeUpdateAction action();

        @Nullable abstract CodeUpdateResponse response();

        @Nullable abstract IdCardData idCardData();

        @Nullable abstract Token token();

        @Nullable abstract Boolean success();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .codeUpdateState(state())
                    .codeUpdateAction(action())
                    .codeUpdateResponse(response());
            if (idCardData() != null && token() != null) {
                builder.idCardDataResponse(IdCardDataResponse.success(idCardData(), token()));
            }
            Boolean success = success();
            if (success != null) {
                builder.codeUpdateSuccessMessageVisible(success);
            }
            return builder.build();
        }

        static CodeUpdateResult action(CodeUpdateAction action) {
            return create(State.IDLE, action, null, null, null, null);
        }

        static CodeUpdateResult progress(CodeUpdateAction action) {
            return create(State.ACTIVE, action, null, null, null, null);
        }

        static CodeUpdateResult response(CodeUpdateAction action, CodeUpdateResponse response,
                                         @Nullable IdCardData idCardData, @Nullable Token token) {
            return create(State.IDLE, action, response, idCardData, token, null);
        }

        static CodeUpdateResult clearResponse(CodeUpdateAction action, CodeUpdateResponse response,
                                              @Nullable IdCardData idCardData,
                                              @Nullable Token token) {
            return create(State.CLEAR, action, response, idCardData, token, null);
        }

        static CodeUpdateResult successResponse(CodeUpdateAction action,
                                                CodeUpdateResponse response,
                                                @Nullable IdCardData idCardData,
                                                @Nullable Token token) {
            return create(State.IDLE, action, response, idCardData, token, true);
        }

        static CodeUpdateResult hideSuccessResponse(CodeUpdateAction action,
                                                    CodeUpdateResponse response,
                                                    @Nullable IdCardData idCardData,
                                                    @Nullable Token token) {
            return create(State.IDLE, action, response, idCardData, token, false);
        }

        static CodeUpdateResult clear() {
            return create(State.IDLE, null, null, null, null, null);
        }

        private static CodeUpdateResult create(@State String state,
                                               @Nullable CodeUpdateAction action,
                                               @Nullable CodeUpdateResponse response,
                                               @Nullable IdCardData idCardData,
                                               @Nullable Token token, @Nullable Boolean success) {
            return new AutoValue_Result_CodeUpdateResult(state, action, response, idCardData,
                    token, success);
        }
    }
}
