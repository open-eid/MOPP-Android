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

        @Nullable abstract Integer signatureAddMethod();

        abstract boolean signatureAddSuccessMessageVisible();

        static InitialIntent create(boolean isExistingContainer, File containerFile,
                                    @Nullable Integer signatureAddMethod,
                                    boolean signatureAddSuccessMessageVisible) {
            return new AutoValue_Intent_InitialIntent(isExistingContainer, containerFile,
                    signatureAddMethod, signatureAddSuccessMessageVisible);
        }
    }

    @AutoValue
    abstract class NameUpdateIntent implements Intent, Action {

        @Nullable abstract File containerFile();

        @Nullable abstract String name();

        static NameUpdateIntent show(File file) {
            return create(file, null);
        }

        static NameUpdateIntent update(File file, String name) {
            return create(file, name);
        }

        static NameUpdateIntent clear() {
            return create(null, null);
        }

        private static NameUpdateIntent create(@Nullable File containerFile,
                                               @Nullable String name) {
            return new AutoValue_Intent_NameUpdateIntent(containerFile, name);
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

        @Nullable abstract Integer method();

        @Nullable abstract Boolean existingContainer();

        @Nullable abstract File containerFile();

        @Nullable abstract SignatureAddRequest request();

        static SignatureAddIntent show(int method, boolean existingContainer, File containerFile) {
            return create(method, existingContainer, containerFile, null);
        }

        static SignatureAddIntent sign(int method, boolean existingContainer, File containerFile,
                                       SignatureAddRequest request) {
            return create(method, existingContainer, containerFile, request);
        }

        static SignatureAddIntent clear() {
            return create(null, null, null, null);
        }

        private static SignatureAddIntent create(@Nullable Integer method,
                                                 @Nullable Boolean existingContainer,
                                                 @Nullable File containerFile,
                                                 @Nullable SignatureAddRequest request) {
            return new AutoValue_Intent_SignatureAddIntent(method, existingContainer, containerFile,
                    request);
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
