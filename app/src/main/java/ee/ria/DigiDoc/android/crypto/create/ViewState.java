package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;

@AutoValue
abstract class ViewState implements MviViewState {

    @Nullable abstract File containerFile();

    @Nullable abstract String name();
    abstract ImmutableList<File> dataFiles();
    abstract boolean dataFilesViewEnabled();
    abstract boolean dataFilesAddEnabled();
    abstract boolean dataFilesRemoveEnabled();
    abstract ImmutableList<Certificate> recipients();
    abstract boolean recipientsAddEnabled();
    abstract boolean recipientsRemoveEnabled();
    abstract boolean encryptButtonVisible();
    abstract boolean decryptButtonVisible();
    abstract boolean sendButtonVisible();

    @State abstract String dataFilesAddState();
    @Nullable abstract Throwable dataFilesAddError();

    @State abstract String dataFileRemoveState();
    @Nullable abstract Throwable dataFileRemoveError();

    @State abstract String recipientsSearchState();
    abstract ImmutableList<Certificate> recipientsSearchResult();
    @Nullable abstract Throwable recipientsSearchError();

    @State abstract String recipientAddState();
    @Nullable abstract Throwable recipientAddError();

    @State abstract String encryptState();
    abstract boolean encryptSuccessMessageVisible();
    @Nullable abstract Throwable encryptError();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .dataFiles(ImmutableList.of())
                .dataFilesViewEnabled(false)
                .dataFilesAddEnabled(false)
                .dataFilesRemoveEnabled(false)
                .recipients(ImmutableList.of())
                .recipientsAddEnabled(false)
                .recipientsRemoveEnabled(false)
                .encryptButtonVisible(false)
                .decryptButtonVisible(false)
                .sendButtonVisible(false)
                .dataFilesAddState(State.IDLE)
                .dataFileRemoveState(State.IDLE)
                .recipientsSearchState(State.IDLE)
                .recipientsSearchResult(ImmutableList.of())
                .recipientAddState(State.IDLE)
                .encryptState(State.IDLE)
                .encryptSuccessMessageVisible(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder containerFile(@Nullable File containerFile);
        Builder name(@Nullable String name);
        Builder dataFiles(ImmutableList<File> dataFiles);
        Builder dataFilesViewEnabled(boolean dataFilesViewEnabled);
        Builder dataFilesAddEnabled(boolean dataFilesAddEnabled);
        Builder dataFilesRemoveEnabled(boolean dataFilesRemoveEnabled);
        Builder recipients(ImmutableList<Certificate> recipients);
        Builder recipientsAddEnabled(boolean recipientsAddEnabled);
        Builder recipientsRemoveEnabled(boolean recipientsRemoveEnabled);
        Builder encryptButtonVisible(boolean encryptButtonVisible);
        Builder decryptButtonVisible(boolean decryptButtonVisible);
        Builder sendButtonVisible(boolean sendButtonVisible);
        Builder dataFilesAddState(@State String dataFilesAddState);
        Builder dataFilesAddError(@Nullable Throwable dataFilesAddError);
        Builder dataFileRemoveState(@State String dataFileRemoveState);
        Builder dataFileRemoveError(@Nullable Throwable dataFileRemoveError);
        Builder recipientsSearchState(@State String recipientsSearchState);
        Builder recipientsSearchResult(ImmutableList<Certificate> recipientsSearchResult);
        Builder recipientsSearchError(@Nullable Throwable recipientsSearchError);
        Builder recipientAddState(@State String recipientAddState);
        Builder recipientAddError(@Nullable Throwable recipientAddError);
        Builder encryptState(@State String encryptState);
        Builder encryptSuccessMessageVisible(boolean encryptSuccessMessageVisible);
        Builder encryptError(@Nullable Throwable encryptError);
        ViewState build();
    }
}
