package ee.ria.DigiDoc.android.crypto.create;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.SivaUtil;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.DigiDoc.common.Certificate;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract android.content.Intent intent();

        abstract boolean isFromSignatureView();

        static InitialIntent create(@Nullable File containerFile,
                                    @Nullable android.content.Intent intent,
                                    boolean isFromSignatureView) {
            return new AutoValue_Intent_InitialIntent(containerFile, intent, isFromSignatureView);
        }
    }

    @AutoValue
    abstract class NameUpdateIntent implements Intent {

        @Nullable abstract String name();

        @Nullable abstract String newName();

        static NameUpdateIntent show(String name) {
            return create(name, null);
        }

        static NameUpdateIntent update(String oldName, String newName) {
            return create(oldName, newName);
        }

        static NameUpdateIntent clear(String oldName) {
            return create(null, oldName);
        }

        private static NameUpdateIntent create(String name, String newName) {
            return new AutoValue_Intent_NameUpdateIntent(name, newName);
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

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        abstract ImmutableList<File> dataFiles();

        @Nullable abstract File dataFile();

        static DataFileRemoveIntent showConfirmation(ImmutableList<File> dataFiles, File dataFile) {
            return create(true, null,  dataFiles, dataFile);
        }

        static DataFileRemoveIntent remove(File containerFile, ImmutableList<File> dataFiles, File dataFile) {
            return create(false, containerFile, dataFiles, dataFile);
        }

        static DataFileRemoveIntent clear(ImmutableList<File> dataFiles) {
            return create(false, null, dataFiles, null);
        }

        private static DataFileRemoveIntent create(boolean showConfirmation, File containerFile, ImmutableList<File> dataFiles, File dataFile) {
            return new AutoValue_Intent_DataFileRemoveIntent(showConfirmation,containerFile, dataFiles, dataFile);
        }
    }

    @AutoValue
    abstract class DataFileSaveIntent implements Intent {

        abstract File dataFile();

        static DataFileSaveIntent create(File dataFile) {
            return new AutoValue_Intent_DataFileSaveIntent(dataFile);
        }
    }

    @AutoValue
    abstract class DataFileViewIntent implements Intent {

        @Nullable abstract File dataFile();

        abstract boolean confirmation();

        static Observable<DataFileViewIntent> confirmation(File containerFile, Context context) {
            return SivaUtil.isSivaConfirmationNeeded(
                            ImmutableList.of(FileStream.create(containerFile)), context)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(isSivaConfirmationNeeded -> create(containerFile, isSivaConfirmationNeeded))
                    .subscribeOn(AndroidSchedulers.mainThread());
        }

        static DataFileViewIntent open(File dataFile) {
            return new AutoValue_Intent_DataFileViewIntent(dataFile, false);
        }

        static DataFileViewIntent cancel() {
            return new AutoValue_Intent_DataFileViewIntent(null, false);
        }

        static DataFileViewIntent create(File dataFile, boolean isConfirmationNeeded) {
            return new AutoValue_Intent_DataFileViewIntent(dataFile, isConfirmationNeeded);
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
    abstract class RecipientsScreenDoneButtonClickIntent implements Intent {

        static RecipientsScreenDoneButtonClickIntent create() {
            return new AutoValue_Intent_RecipientsScreenDoneButtonClickIntent();
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
    abstract class RecipientAddAllIntent implements Intent {

        abstract ImmutableList<Certificate> recipients();

        abstract ImmutableList<Certificate> addedRecipients();

        static RecipientAddAllIntent create(ImmutableList<Certificate> recipients,
                                            ImmutableList<Certificate> addedRecipients) {
            return new AutoValue_Intent_RecipientAddAllIntent(recipients, addedRecipients);
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

    @AutoValue
    abstract class ContainerSaveIntent implements Intent {

        abstract File containerFile();

        static ContainerSaveIntent create(File containerFile) {
            return new AutoValue_Intent_ContainerSaveIntent(containerFile);
        }
    }

    @AutoValue
    abstract class SignIntent implements Intent {

        abstract File containerFile();

        static SignIntent create(File containerFile) {
            return new AutoValue_Intent_SignIntent(containerFile);
        }
    }
}
