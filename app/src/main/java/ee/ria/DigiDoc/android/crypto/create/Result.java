package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class VoidResult implements Result {

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static VoidResult create() {
            return new AutoValue_Result_VoidResult();
        }
    }

    @AutoValue
    abstract class RecipientsSearchResult implements Result {

        @State abstract String state();

        abstract ImmutableList<Certificate> result();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .recipientsSearchState(state())
                    .recipientsSearchResult(result())
                    .recipientsSearchError(error())
                    .build();
        }

        static RecipientsSearchResult activity() {
            return create(State.ACTIVE, ImmutableList.of(), null);
        }

        static RecipientsSearchResult success(ImmutableList<Certificate> result) {
            return create(State.IDLE, result, null);
        }

        static RecipientsSearchResult failure(Throwable error) {
            return create(State.IDLE, ImmutableList.of(), error);
        }

        private static RecipientsSearchResult create(
                @State String state, ImmutableList<Certificate> result, @Nullable Throwable error) {
            return new AutoValue_Result_RecipientsSearchResult(state, result, error);
        }
    }
}
