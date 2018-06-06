package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.cryptolib.Recipient;

@AutoValue
abstract class ViewState implements MviViewState {

    @State abstract String recipientsSearchState();
    @Nullable abstract ImmutableList<Recipient> recipientsSearchResult();
    @Nullable abstract Throwable recipientsSearchError();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .recipientsSearchState(State.IDLE)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder recipientsSearchState(@State String recipientsSearchState);
        Builder recipientsSearchResult(@Nullable ImmutableList<Recipient> recipientsSearchResult);
        Builder recipientsSearchError(@Nullable Throwable recipientsSearchError);
        ViewState build();
    }
}
