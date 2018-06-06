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

        static RecipientsSearchResult clear() {
            return create(State.IDLE, ImmutableList.of(), null);
        }

        private static RecipientsSearchResult create(
                @State String state, ImmutableList<Certificate> result, @Nullable Throwable error) {
            return new AutoValue_Result_RecipientsSearchResult(state, result, error);
        }
    }

    @AutoValue
    abstract class RecipientAddResult implements Result {

        @State abstract String state();

        @Nullable abstract ImmutableList<Certificate> recipients();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .recipientAddState(state())
                    .recipientAddError(error());
            if (recipients() != null) {
                builder.recipients(recipients());
            }
            return builder.build();
        }

        static RecipientAddResult activity() {
            return create(State.ACTIVE, null, null);
        }

        static RecipientAddResult success(ImmutableList<Certificate> recipients) {
            return create(State.IDLE, recipients, null);
        }

        static RecipientAddResult failure(Throwable error) {
            return create(State.IDLE, null, error);
        }

        private static RecipientAddResult create(@State String state,
                                                 @Nullable ImmutableList<Certificate> recipients,
                                                 @Nullable Throwable error) {
            return new AutoValue_Result_RecipientAddResult(state, recipients, error);
        }
    }

    @AutoValue
    abstract class RecipientRemoveResult implements Result {

        @State abstract String state();

        @Nullable abstract ImmutableList<Certificate> recipients();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .recipientAddState(state())
                    .recipientAddError(error());
            if (recipients() != null) {
                builder.recipients(recipients());
            }
            return builder.build();
        }

        static RecipientRemoveResult activity() {
            return create(State.ACTIVE, null, null);
        }

        static RecipientRemoveResult success(ImmutableList<Certificate> recipients) {
            return create(State.IDLE, recipients, null);
        }

        static RecipientRemoveResult failure(Throwable error) {
            return create(State.IDLE, null, error);
        }

        private static RecipientRemoveResult create(@State String state,
                                                 @Nullable ImmutableList<Certificate> recipients,
                                                 @Nullable Throwable error) {
            return new AutoValue_Result_RecipientRemoveResult(state, recipients, error);
        }
    }
}
