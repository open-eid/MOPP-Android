package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        abstract boolean isExistingContainer();

        abstract File containerFile();

        abstract boolean signatureAddVisible();

        abstract boolean signatureAddSuccessMessageVisible();

        static InitialIntent create(boolean isExistingContainer, File containerFile,
                                    boolean signatureAddVisible,
                                    boolean signatureAddSuccessMessageVisible) {
            return new AutoValue_Intent_InitialIntent(isExistingContainer, containerFile,
                    signatureAddVisible, signatureAddSuccessMessageVisible);
        }
    }

    @AutoValue
    abstract class DocumentsAddIntent implements Intent {

        @Nullable abstract File containerFile();

        static DocumentsAddIntent create(File containerFile) {
            return new AutoValue_Intent_DocumentsAddIntent(containerFile);
        }

        static DocumentsAddIntent clear() {
            return new AutoValue_Intent_DocumentsAddIntent(null);
        }
    }

    @AutoValue
    abstract class DocumentOpenIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract DataFile document();

        static DocumentOpenIntent open(File containerFile, DataFile document) {
            return new AutoValue_Intent_DocumentOpenIntent(containerFile, document);
        }

        static DocumentOpenIntent clear() {
            return new AutoValue_Intent_DocumentOpenIntent(null, null);
        }
    }

    @AutoValue
    abstract class DocumentRemoveIntent implements Intent {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        @Nullable abstract DataFile document();

        static DocumentRemoveIntent showConfirmation(File containerFile, DataFile document) {
            return new AutoValue_Intent_DocumentRemoveIntent(true, containerFile, document);
        }

        static DocumentRemoveIntent remove(File containerFile, DataFile document) {
            return new AutoValue_Intent_DocumentRemoveIntent(false, containerFile, document);
        }

        static DocumentRemoveIntent clear() {
            return new AutoValue_Intent_DocumentRemoveIntent(false, null, null);
        }
    }

    @AutoValue
    abstract class SignatureRemoveIntent implements Intent {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        @Nullable abstract Signature signature();

        static SignatureRemoveIntent showConfirmation(File containerFile, Signature signature) {
            return new AutoValue_Intent_SignatureRemoveIntent(true, containerFile, signature);
        }

        static SignatureRemoveIntent remove(File containerFile, Signature signature) {
            return new AutoValue_Intent_SignatureRemoveIntent(false, containerFile, signature);
        }

        static SignatureRemoveIntent clear() {
            return new AutoValue_Intent_SignatureRemoveIntent(false, null, null);
        }
    }

    @AutoValue
    abstract class SignatureAddIntent implements Intent {

        abstract boolean show();

        abstract boolean isExistingContainer();

        @Nullable abstract File containerFile();

        @Nullable abstract String phoneNo();

        @Nullable abstract String personalCode();

        @Nullable abstract Boolean rememberMe();

        static SignatureAddIntent showIntent(boolean isExistingContainer, File containerFile) {
            return new AutoValue_Intent_SignatureAddIntent(true, isExistingContainer, containerFile,
                    null, null, null);
        }

        static SignatureAddIntent addIntent(boolean isExistingContainer, File containerFile,
                                            String phoneNo, String personalCode,
                                            boolean rememberMe) {
            return new AutoValue_Intent_SignatureAddIntent(false, isExistingContainer,
                    containerFile, phoneNo, personalCode, rememberMe);
        }

        static SignatureAddIntent clearIntent() {
            return new AutoValue_Intent_SignatureAddIntent(false, false, null, null, null, null);
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
