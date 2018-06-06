package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.cryptolib.Recipient;

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

        @Nullable abstract ImmutableList<Recipient> recipients();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .recipientsSearchState(state())
                    .recipientsSearchResult(recipients())
                    .recipientsSearchError(error())
                    .build();
        }

        static RecipientsSearchResult activity() {
            return create(State.ACTIVE, null, null);
        }

        static RecipientsSearchResult success(ImmutableList<Recipient> recipients) {
            return create(State.IDLE, recipients, null);
        }

        static RecipientsSearchResult failure(Throwable error) {
            return create(State.IDLE, null, error);
        }

        private static RecipientsSearchResult create(
                @State String state, @Nullable ImmutableList<Recipient> recipients,
                @Nullable Throwable error) {
            return new AutoValue_Result_RecipientsSearchResult(state, recipients, error);
        }
    }
}
