package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.common.Certificate;

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

    @State abstract String recipientsSearchState();
    @Nullable abstract ImmutableList<Certificate> recipientsSearchResult();
    @Nullable abstract Throwable recipientsSearchError();

    @State abstract String encryptState();
    abstract boolean encryptSuccessMessageVisible();
    @Nullable abstract Throwable encryptError();

    @Nullable abstract IdCardDataResponse decryptionIdCardDataResponse();

    @State abstract String decryptState();
    abstract boolean decryptSuccessMessageVisible();
    @Nullable abstract Throwable decryptError();

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
                .recipientsSearchState(State.IDLE)
                .encryptState(State.IDLE)
                .encryptSuccessMessageVisible(false)
                .decryptState(State.IDLE)
                .decryptSuccessMessageVisible(false)
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
        Builder recipientsSearchState(@State String recipientsSearchState);
        Builder recipientsSearchResult(@Nullable ImmutableList<Certificate> recipientsSearchResult);
        Builder recipientsSearchError(@Nullable Throwable recipientsSearchError);
        Builder encryptState(@State String encryptState);
        Builder encryptSuccessMessageVisible(boolean encryptSuccessMessageVisible);
        Builder encryptError(@Nullable Throwable encryptError);
        Builder decryptionIdCardDataResponse(
                @Nullable IdCardDataResponse decryptionIdCardDataResponse);
        Builder decryptState(@State String decryptState);
        Builder decryptSuccessMessageVisible(boolean decryptSuccessMessageVisible);
        Builder decryptError(@Nullable Throwable decryptError);
        ViewState build();
    }
}
