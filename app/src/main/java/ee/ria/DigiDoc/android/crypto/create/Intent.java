package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.DigiDoc.common.Certificate;

interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract android.content.Intent intent();

        static InitialIntent create(@Nullable File containerFile,
                                    @Nullable android.content.Intent intent) {
            return new AutoValue_Intent_InitialIntent(containerFile, intent);
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

        @Nullable abstract ImmutableList<File> dataFiles();

        static DataFilesAddIntent start(ImmutableList<File> dataFiles) {
            return create(dataFiles);
        }

        static DataFilesAddIntent clear() {
            return create(null);
        }

        private static DataFilesAddIntent create(@Nullable ImmutableList<File> dataFiles) {
            return new AutoValue_Intent_DataFilesAddIntent(dataFiles);
        }
    }

    @AutoValue
    abstract class DataFileRemoveIntent implements Intent {

        abstract ImmutableList<File> dataFiles();

        abstract File dataFile();

        static DataFileRemoveIntent create(ImmutableList<File> dataFiles, File dataFile) {
            return new AutoValue_Intent_DataFileRemoveIntent(dataFiles, dataFile);
        }
    }

    @AutoValue
    abstract class DataFileViewIntent implements Intent {

        abstract File dataFile();

        static DataFileViewIntent create(File dataFile) {
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

        @Nullable abstract String name();

        @Nullable abstract ImmutableList<File> dataFiles();

        @Nullable abstract ImmutableList<Certificate> recipients();

        static EncryptIntent start(String name, ImmutableList<File> dataFiles,
                                   ImmutableList<Certificate> recipients) {
            return create(name, dataFiles, recipients);
        }

        static EncryptIntent clear() {
            return create(null, null, null);
        }

        private static EncryptIntent create(@Nullable String name,
                                            @Nullable ImmutableList<File> dataFiles,
                                            @Nullable ImmutableList<Certificate> recipients) {
            return new AutoValue_Intent_EncryptIntent(name, dataFiles, recipients);
        }
    }

    @AutoValue
    abstract class DecryptionIntent implements Intent {

        abstract boolean visible();

        static DecryptionIntent show() {
            return create(true);
        }

        static DecryptionIntent hide() {
            return create(false);
        }

        private static DecryptionIntent create(boolean visible) {
            return new AutoValue_Intent_DecryptionIntent(visible);
        }
    }

    @AutoValue
    abstract class DecryptIntent implements Intent {

        @Nullable abstract DecryptRequest request();

        static DecryptIntent start(DecryptRequest request) {
            return create(request);
        }

        static DecryptIntent cancel() {
            return create(null);
        }

        private static DecryptIntent create(@Nullable DecryptRequest request) {
            return new AutoValue_Intent_DecryptIntent(request);
        }
    }

    @AutoValue
    abstract class SendIntent implements Intent {

        abstract File containerFile();

        static SendIntent create(File containerFile) {
            return new AutoValue_Intent_SendIntent(containerFile);
        }
    }
}
