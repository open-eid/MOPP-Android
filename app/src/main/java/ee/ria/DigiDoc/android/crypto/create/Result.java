package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.cryptolib.CryptoContainer;

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
    abstract class InitialResult implements Result {

        @State abstract String state();

        @Nullable abstract File containerFile();
        @Nullable abstract ImmutableList<File> dataFiles();

        @Nullable abstract CryptoContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            File containerFile = containerFile();
            ImmutableList<File> dataFiles = dataFiles();
            CryptoContainer container = container();

            ViewState.Builder builder = state.buildWith()
                    .dataFilesAddState(state())
                    .dataFilesAddError(error())
                    .dataFilesViewEnabled(false)
                    .dataFilesAddEnabled(false)
                    .dataFilesRemoveEnabled(false)
                    .recipientsAddEnabled(false)
                    .recipientsRemoveEnabled(false)
                    .encryptButtonVisible(false)
                    .decryptButtonVisible(false)
                    .sendButtonVisible(false);

            if (containerFile != null && dataFiles != null) {
                builder
                        .name(containerFile.getName())
                        .dataFiles(dataFiles)
                        .dataFilesViewEnabled(true)
                        .dataFilesAddEnabled(true)
                        .dataFilesRemoveEnabled(true)
                        .recipientsAddEnabled(true)
                        .recipientsRemoveEnabled(true)
                        .encryptButtonVisible(true);
            } else if (container != null) {
                builder
                        .name(container.file().getName())
                        .dataFiles(container.dataFiles())
                        .recipients(container.recipients())
                        .decryptButtonVisible(true)
                        .sendButtonVisible(true);
            }

            return builder.build();
        }

        static InitialResult activity() {
            return create(State.ACTIVE, null, null, null, null);
        }

        static InitialResult success(File containerFile, ImmutableList<File> dataFiles) {
            return create(State.IDLE, containerFile, dataFiles, null, null);
        }

        static InitialResult success(CryptoContainer container) {
            return create(State.IDLE, null, null, container, null);
        }

        static InitialResult failure(Throwable error) {
            return create(State.IDLE, null, null, null, error);
        }

        static InitialResult clear() {
            return create(State.IDLE, null, null, null, null);
        }

        private static InitialResult create(@State String state, @Nullable File containerFile,
                                            @Nullable ImmutableList<File> dataFiles,
                                            @Nullable CryptoContainer container,
                                            @Nullable Throwable error) {
            return new AutoValue_Result_InitialResult(state, containerFile, dataFiles, container,
                    error);
        }
    }

    @AutoValue
    abstract class DataFilesAddResult implements Result {

        @State abstract String state();

        @Nullable abstract ImmutableList<File> dataFiles();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .dataFilesAddState(state())
                    .dataFilesAddError(error());
            if (dataFiles() != null) {
                builder.dataFiles(dataFiles());
            }
            return builder.build();
        }

        static DataFilesAddResult activity() {
            return create(State.ACTIVE, null, null);
        }

        static DataFilesAddResult success(ImmutableList<File> dataFiles) {
            return create(State.IDLE, dataFiles, null);
        }

        static DataFilesAddResult failure(Throwable error) {
            return create(State.IDLE, null, error);
        }

        static DataFilesAddResult clear() {
            return create(State.IDLE, null, null);
        }

        private static DataFilesAddResult create(@State String state,
                                                 @Nullable ImmutableList<File> dataFiles,
                                                 @Nullable Throwable error) {
            return new AutoValue_Result_DataFilesAddResult(state, dataFiles, error);
        }
    }

    @AutoValue
    abstract class DataFileRemoveResult implements Result {

        @State abstract String state();

        @Nullable abstract ImmutableList<File> dataFiles();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .dataFileRemoveState(state())
                    .dataFileRemoveError(error());
            if (dataFiles() != null) {
                builder.dataFiles(dataFiles());
            }
            return builder.build();
        }

        static DataFileRemoveResult activity() {
            // set it as idle since it takes so little time and causes flickering in the UI
            return create(State.IDLE, null, null);
        }

        static DataFileRemoveResult success(ImmutableList<File> dataFiles) {
            return create(State.IDLE, dataFiles, null);
        }

        static DataFileRemoveResult failure(Throwable error) {
            return create(State.IDLE, null, error);
        }

        private static DataFileRemoveResult create(@State String state,
                                                   @Nullable ImmutableList<File> dataFiles,
                                                   @Nullable Throwable error) {
            return new AutoValue_Result_DataFileRemoveResult(state, dataFiles, error);
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

    @AutoValue
    abstract class EncryptResult implements Result {

        @State abstract String state();

        abstract boolean successMessageVisible();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .encryptState(state())
                    .encryptSuccessMessageVisible(successMessageVisible())
                    .encryptError(error())
                    .build();
        }

        static EncryptResult activity() {
            return create(State.ACTIVE, false, null);
        }

        static EncryptResult successMessage() {
            return create(State.IDLE, true, null);
        }

        static EncryptResult clear() {
            return create(State.IDLE, false, null);
        }

        static EncryptResult failure(Throwable error) {
            return create(State.IDLE, false, error);
        }

        private static EncryptResult create(@State String state, boolean successMessageVisible,
                                            @Nullable Throwable error) {
            return new AutoValue_Result_EncryptResult(state, successMessageVisible, error);
        }
    }
}
