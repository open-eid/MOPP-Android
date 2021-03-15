package ee.ria.DigiDoc.android.crypto.create;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.sign.DataFile;

interface Result extends MviResult<ViewState> {

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
                        .containerFile(container.file())
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
    abstract class NameUpdateResult implements Result {

        @Nullable abstract String name();

        @Nullable abstract String newName();

        abstract boolean inProgress();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .nameUpdateShowing(name() != null)
                    .newName(newName())
                    .nameUpdateInProgress(inProgress())
                    .nameUpdateError(error())
                    .build();
        }

        static NameUpdateResult show(String name) {
            return create(name, null,false, null);
        }

        static NameUpdateResult progress(String newName) {
            return create(null, newName, true, null);
        }

        static NameUpdateResult failure(String name, Throwable error) {
            return create(name, null, false, error);
        }

        private static NameUpdateResult create(@Nullable String name, @Nullable String newName,
                                               boolean inProgress, @Nullable Throwable error) {
            return new AutoValue_Result_NameUpdateResult(name, newName, inProgress, error);
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

        abstract ImmutableList<File> dataFiles();

        @Nullable abstract File showConfirmation();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .dataFiles(dataFiles())
                    .dataFileRemoveConfirmation(showConfirmation())
                    .build();
        }

        static DataFileRemoveResult confirmation(ImmutableList<File> dataFiles, File dataFile) {
            return new AutoValue_Result_DataFileRemoveResult(dataFiles , dataFile);
        }

        static DataFileRemoveResult success(ImmutableList<File> dataFiles) {
            return new AutoValue_Result_DataFileRemoveResult(dataFiles, null);
        }

        static DataFileRemoveResult clear(ImmutableList<File> dataFiles) {
            return new AutoValue_Result_DataFileRemoveResult(dataFiles, null);
        }
    }

    @AutoValue
    abstract class RecipientsAddButtonClickResult implements Result {

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().recipientsSearchResult(null).build();
        }

        static RecipientsAddButtonClickResult create() {
            return new AutoValue_Result_RecipientsAddButtonClickResult();
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

        abstract ImmutableList<Certificate> recipients();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().recipients(recipients()).build();
        }

        static RecipientAddResult create(ImmutableList<Certificate> recipients) {
            return new AutoValue_Result_RecipientAddResult(recipients);
        }
    }

    @AutoValue
    abstract class RecipientRemoveResult implements Result {

        abstract ImmutableList<Certificate> recipients();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().recipients(recipients()).build();
        }

        static RecipientRemoveResult create(ImmutableList<Certificate> recipients) {
            return new AutoValue_Result_RecipientRemoveResult(recipients);
        }
    }

    @AutoValue
    abstract class EncryptResult implements Result {

        @State abstract String state();

        abstract boolean successMessageVisible();

        @Nullable abstract File containerFile();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .encryptState(state())
                    .encryptSuccessMessageVisible(successMessageVisible())
                    .encryptError(error());
            if (containerFile() != null) {
                builder
                        .containerFile(containerFile())
                        .dataFilesViewEnabled(false)
                        .dataFilesAddEnabled(false)
                        .dataFilesRemoveEnabled(false)
                        .recipientsAddEnabled(false)
                        .recipientsRemoveEnabled(false)
                        .encryptButtonVisible(false)
                        .decryptButtonVisible(true)
                        .sendButtonVisible(true);
            }
            return builder.build();
        }

        static EncryptResult activity() {
            return create(State.ACTIVE, false, null, null);
        }

        static EncryptResult successMessage(File containerFile) {
            return create(State.IDLE, true, containerFile, null);
        }

        static EncryptResult success(File containerFile) {
            return create(State.IDLE, false, containerFile, null);
        }

        static EncryptResult failure(Throwable error) {
            return create(State.IDLE, false, null, error);
        }

        static EncryptResult clear() {
            return create(State.IDLE, false, null, null);
        }

        private static EncryptResult create(@State String state, boolean successMessageVisible,
                                            @Nullable File containerFile,
                                            @Nullable Throwable error) {
            return new AutoValue_Result_EncryptResult(state, successMessageVisible, containerFile,
                    error);
        }
    }

    @AutoValue
    abstract class DecryptionResult implements Result {

        @Nullable abstract IdCardDataResponse idCardDataResponse();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .decryptionIdCardDataResponse(idCardDataResponse());
            if (idCardDataResponse() == null) {
                builder.decryptError(null);
            }
            return builder.build();
        }

        static DecryptionResult show(IdCardDataResponse idCardDataResponse) {
            return create(idCardDataResponse);
        }

        static DecryptionResult hide() {
            return create(null);
        }

        private static DecryptionResult create(@Nullable IdCardDataResponse idCardDataResponse) {
            return new AutoValue_Result_DecryptionResult(idCardDataResponse);
        }
    }

    @AutoValue
    abstract class DecryptResult implements Result {

        @State abstract String state();

        abstract boolean successMessageVisible();

        @Nullable abstract ImmutableList<File> dataFiles();

        @Nullable abstract Throwable error();

        @Nullable abstract IdCardDataResponse idCardDataResponse();

        @Override
        public ViewState reduce(ViewState state) {
            IdCardDataResponse idCardDataResponse = idCardDataResponse();
            IdCardData idCardData = idCardDataResponse != null ? idCardDataResponse.data() : null;
            int pin1RetryCount = idCardData != null ? idCardData.pin1RetryCount() : -1;
            Throwable error = error();
            ViewState.Builder builder = state.buildWith()
                    .decryptState(state())
                    .decryptSuccessMessageVisible(successMessageVisible())
                    .decryptError(error);
            if (dataFiles() != null) {
                builder
                        .dataFiles(dataFiles())
                        .dataFilesViewEnabled(true)
                        .encryptButtonVisible(false)
                        .decryptButtonVisible(false)
                        .sendButtonVisible(false)
                        .decryptionIdCardDataResponse(null)
                        .decryptState(State.IDLE)
                        .decryptError(null);
            }
            if (error != null && error instanceof Pin1InvalidException && pin1RetryCount > 0) {
                builder.decryptionIdCardDataResponse(idCardDataResponse);
            } else if (error != null) {
                builder.decryptionIdCardDataResponse(null);
            }
            return builder.build();
        }

        static DecryptResult activity() {
            return create(State.ACTIVE, false, null, null, null);
        }

        static DecryptResult clear() {
            return create(State.CLEAR, false, null, null, null);
        }

        static DecryptResult idle() {
            return create(State.IDLE, false, null, null, null);
        }

        static DecryptResult successMessage(ImmutableList<File> dataFiles) {
            return create(State.IDLE, true, dataFiles, null, null);
        }

        static DecryptResult success(ImmutableList<File> dataFiles) {
            return create(State.IDLE, false, dataFiles, null, null);
        }

        static DecryptResult failure(Throwable error,
                                     @Nullable IdCardDataResponse idCardDataResponse) {
            return create(State.IDLE, false, null, error, idCardDataResponse);
        }

        private static DecryptResult create(@State String state, boolean successMessageVisible,
                                            @Nullable ImmutableList<File> dataFiles,
                                            @Nullable Throwable error,
                                            @Nullable IdCardDataResponse idCardDataResponse) {
            return new AutoValue_Result_DecryptResult(state, successMessageVisible, dataFiles,
                    error, idCardDataResponse);
        }
    }
}
