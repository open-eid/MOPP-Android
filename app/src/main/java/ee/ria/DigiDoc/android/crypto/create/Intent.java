package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.cryptolib.DataFile;

interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class UpButtonClickIntent implements Intent {

        static UpButtonClickIntent create() {
            return new AutoValue_Intent_UpButtonClickIntent();
        }
    }

    @AutoValue
    abstract class DataFilesAddIntent implements Intent {

        @Nullable abstract ImmutableList<DataFile> dataFiles();

        static DataFilesAddIntent start(ImmutableList<DataFile> dataFiles) {
            return create(dataFiles);
        }

        static DataFilesAddIntent clear() {
            return create(null);
        }

        private static DataFilesAddIntent create(@Nullable ImmutableList<DataFile> dataFiles) {
            return new AutoValue_Intent_DataFilesAddIntent(dataFiles);
        }
    }

    @AutoValue
    abstract class DataFileRemoveIntent implements Intent {

        abstract ImmutableList<DataFile> dataFiles();

        abstract DataFile dataFile();

        static DataFileRemoveIntent create(ImmutableList<DataFile> dataFiles, DataFile dataFile) {
            return new AutoValue_Intent_DataFileRemoveIntent(dataFiles, dataFile);
        }
    }

    @AutoValue
    abstract class DataFileViewIntent implements Intent {

        abstract DataFile dataFile();

        static DataFileViewIntent create(DataFile dataFile) {
            return new AutoValue_Intent_DataFileViewIntent(dataFile);
        }
    }

    @AutoValue
    abstract class RecipientsAddButtonClickIntent implements Intent {

        abstract String cryptoCreateScreenId();

        static RecipientsAddButtonClickIntent create(String cryptoCreateScreenId) {
            return new AutoValue_Intent_RecipientsAddButtonClickIntent(cryptoCreateScreenId);
        }
    }

    @AutoValue
    abstract class RecipientsScreenUpButtonClickIntent implements Intent {

        static RecipientsScreenUpButtonClickIntent create() {
            return new AutoValue_Intent_RecipientsScreenUpButtonClickIntent();
        }
    }

    @AutoValue
    abstract class RecipientsSearchIntent implements Intent {

        @Nullable abstract String query();

        static RecipientsSearchIntent search(String query) {
            return create(query);
        }

        static RecipientsSearchIntent clear() {
            return create(null);
        }

        private static RecipientsSearchIntent create(@Nullable String query) {
            return new AutoValue_Intent_RecipientsSearchIntent(query);
        }
    }

    @AutoValue
    abstract class RecipientAddIntent implements Intent {

        abstract ImmutableList<Certificate> recipients();

        abstract Certificate recipient();

        static RecipientAddIntent create(ImmutableList<Certificate> recipients,
                                         Certificate recipient) {
            return new AutoValue_Intent_RecipientAddIntent(recipients, recipient);
        }
    }

    @AutoValue
    abstract class RecipientRemoveIntent implements Intent {

        abstract ImmutableList<Certificate> recipients();

        abstract Certificate recipient();

        static RecipientRemoveIntent create(ImmutableList<Certificate> recipients,
                                         Certificate recipient) {
            return new AutoValue_Intent_RecipientRemoveIntent(recipients, recipient);
        }
    }

    @AutoValue
    abstract class EncryptIntent implements Intent {

        abstract File containerFile();

        abstract ImmutableList<DataFile> dataFiles();

        abstract ImmutableList<Certificate> recipients();

        static EncryptIntent create(File containerFile, ImmutableList<DataFile> dataFiles,
                                    ImmutableList<Certificate> recipients) {
            return new AutoValue_Intent_EncryptIntent(containerFile, dataFiles, recipients);
        }
    }
}
