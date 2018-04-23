package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.tokenlibrary.Token;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class LoadResult implements Result {

        @Nullable abstract IdCardDataResponse idCardDataResponse();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith();

            if (idCardDataResponse() == null && error() == null) {
                builder
                        .idCardDataResponse(IdCardDataResponse.initial())
                        .error(null)
                        .codeUpdateAction(null);
            } else if (idCardDataResponse() != null) {
                builder.idCardDataResponse(idCardDataResponse());
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

        @Nullable abstract CodeUpdateAction action();

        @Nullable abstract CodeUpdateResponse response();

        @Nullable abstract IdCardData idCardData();

        @Nullable abstract Token token();

        abstract boolean inProgress();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .codeUpdateAction(action())
                    .codeUpdateResponse(response())
                    .codeUpdateActivity(inProgress());
            if (idCardData() != null && token() != null) {
                builder.idCardDataResponse(IdCardDataResponse.success(idCardData(), token()));
            }
            return builder.build();
        }

        static CodeUpdateResult action(CodeUpdateAction action) {
            return create(action, null, null, null, false);
        }

        static CodeUpdateResult progress(CodeUpdateAction action) {
            return create(action, null, null, null, true);
        }

        static CodeUpdateResult response(CodeUpdateAction action, CodeUpdateResponse response,
                                         @Nullable IdCardData idCardData, @Nullable Token token) {
            return create(action, response, idCardData, token, false);
        }

        static CodeUpdateResult clear() {
            return create(null, null, null, null, false);
        }

        private static CodeUpdateResult create(@Nullable CodeUpdateAction action,
                                               @Nullable CodeUpdateResponse response,
                                               @Nullable IdCardData idCardData,
                                               @Nullable Token token,
                                               boolean inProgress) {
            return new AutoValue_Result_CodeUpdateResult(action, response, idCardData, token,
                    inProgress);
        }
    }
}
