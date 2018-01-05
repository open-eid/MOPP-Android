package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        abstract File containerFile();

        static InitialIntent create(File containerFile) {
            return new AutoValue_Intent_InitialIntent(containerFile);
        }
    }

    @AutoValue
    abstract class AddDocumentsIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract ImmutableList<FileStream> fileStreams();

        static AddDocumentsIntent pick(File containerFile) {
            return new AutoValue_Intent_AddDocumentsIntent(containerFile, null);
        }

        static AddDocumentsIntent add(File containerFile, ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Intent_AddDocumentsIntent(containerFile, fileStreams);
        }

        static AddDocumentsIntent clear() {
            return new AutoValue_Intent_AddDocumentsIntent(null, null);
        }
    }

    @AutoValue
    abstract class OpenDocumentIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract Document document();

        static OpenDocumentIntent open(File containerFile, Document document) {
            return new AutoValue_Intent_OpenDocumentIntent(containerFile, document);
        }

        static OpenDocumentIntent clear() {
            return new AutoValue_Intent_OpenDocumentIntent(null, null);
        }
    }

    @AutoValue
    abstract class RemoveDocumentIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract Document document();

        static RemoveDocumentIntent create(File containerFile, Document document) {
            return new AutoValue_Intent_RemoveDocumentIntent(containerFile, document);
        }

        static RemoveDocumentIntent clear() {
            return new AutoValue_Intent_RemoveDocumentIntent(null, null);
        }
    }

    @AutoValue
    abstract class SignatureListVisibilityIntent implements Intent {

        abstract boolean isVisible();

        static SignatureListVisibilityIntent create(boolean isVisible) {
            return new AutoValue_Intent_SignatureListVisibilityIntent(isVisible);
        }
    }

    @AutoValue
    abstract class SignatureRemoveSelectionIntent implements Intent {

        @Nullable abstract Signature signature();

        static SignatureRemoveSelectionIntent create(@Nullable Signature signature) {
            return new AutoValue_Intent_SignatureRemoveSelectionIntent(signature);
        }
    }

    @AutoValue
    abstract class SignatureRemoveIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract Signature signature();

        static SignatureRemoveIntent create(File containerFile, Signature signature) {
            return new AutoValue_Intent_SignatureRemoveIntent(containerFile, signature);
        }

        static SignatureRemoveIntent clear() {
            return new AutoValue_Intent_SignatureRemoveIntent(null, null);
        }
    }

    @AutoValue
    abstract class SignatureAddIntent implements Intent {

        abstract boolean show();

        @Nullable abstract File containerFile();

        @Nullable abstract String phoneNo();

        @Nullable abstract String personalCode();

        @Nullable abstract Boolean rememberMe();

        static SignatureAddIntent showIntent(File containerFile) {
            return new AutoValue_Intent_SignatureAddIntent(true, containerFile, null, null, null);
        }

        static SignatureAddIntent addIntent(File containerFile, String phoneNo, String personalCode,
                                            boolean rememberMe) {
            return new AutoValue_Intent_SignatureAddIntent(false, containerFile, phoneNo,
                    personalCode, rememberMe);
        }

        static SignatureAddIntent clearIntent() {
            return new AutoValue_Intent_SignatureAddIntent(false, null, null, null, null);
        }
    }
}
