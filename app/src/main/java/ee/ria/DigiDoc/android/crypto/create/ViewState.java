package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.cryptolib.DataFile;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract ImmutableList<DataFile> dataFiles();
    abstract ImmutableList<Certificate> recipients();

    @State abstract String dataFilesAddState();
    @Nullable abstract Throwable dataFilesAddError();

    @State abstract String recipientsSearchState();
    abstract ImmutableList<Certificate> recipientsSearchResult();
    @Nullable abstract Throwable recipientsSearchError();

    @State abstract String recipientAddState();
    @Nullable abstract Throwable recipientAddError();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .dataFiles(ImmutableList.of())
                .recipients(ImmutableList.of())
                .dataFilesAddState(State.IDLE)
                .recipientsSearchState(State.IDLE)
                .recipientsSearchResult(ImmutableList.of())
                .recipientAddState(State.IDLE)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder dataFiles(ImmutableList<DataFile> dataFiles);
        Builder recipients(ImmutableList<Certificate> recipients);
        Builder dataFilesAddState(@State String dataFilesAddState);
        Builder dataFilesAddError(@Nullable Throwable dataFilesAddError);
        Builder recipientsSearchState(@State String recipientsSearchState);
        Builder recipientsSearchResult(ImmutableList<Certificate> recipientsSearchResult);
        Builder recipientsSearchError(@Nullable Throwable recipientsSearchError);
        Builder recipientAddState(@State String recipientAddState);
        Builder recipientAddError(@Nullable Throwable recipientAddError);
        ViewState build();
    }
}
