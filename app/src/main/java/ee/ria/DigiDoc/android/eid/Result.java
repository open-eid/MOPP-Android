package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;

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
                        .error(null);
            } else if (idCardDataResponse() != null) {
                builder.idCardDataResponse(idCardDataResponse());
            } else if (error() != null) {
                builder.error(error());
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

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static CodeUpdateResult create() {
            return new AutoValue_Result_CodeUpdateResult();
        }
    }
}
