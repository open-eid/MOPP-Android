package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract ImmutableList<Certificate> recipients();

    @State abstract String recipientsSearchState();
    abstract ImmutableList<Certificate> recipientsSearchResult();
    @Nullable abstract Throwable recipientsSearchError();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .recipients(ImmutableList.of())
                .recipientsSearchState(State.IDLE)
                .recipientsSearchResult(ImmutableList.of())
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder recipients(ImmutableList<Certificate> recipients);
        Builder recipientsSearchState(@State String recipientsSearchState);
        Builder recipientsSearchResult(ImmutableList<Certificate> recipientsSearchResult);
        Builder recipientsSearchError(@Nullable Throwable recipientsSearchError);
        ViewState build();
    }
}
