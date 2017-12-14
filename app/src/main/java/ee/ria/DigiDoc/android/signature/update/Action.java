package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class LoadContainerAction implements Action {

        abstract File containerFile();

        static LoadContainerAction create(File containerFile) {
            return new AutoValue_Action_LoadContainerAction(containerFile);
        }
    }

    @AutoValue
    abstract class AddDocumentsAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract ImmutableList<FileStream> fileStreams();

        static AddDocumentsAction create(@Nullable File containerFile,
                                         @Nullable ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Action_AddDocumentsAction(containerFile, fileStreams);
        }
    }

    @AutoValue
    abstract class OpenDocumentAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract Document document();

        static OpenDocumentAction create(@Nullable File containerFile,
                                         @Nullable Document document) {
            return new AutoValue_Action_OpenDocumentAction(containerFile, document);
        }
    }

    @AutoValue
    abstract class DocumentsSelectionAction implements Action {

        @Nullable abstract ImmutableSet<Document> documents();

        static DocumentsSelectionAction create(@Nullable ImmutableSet<Document> documents) {
            return new AutoValue_Action_DocumentsSelectionAction(documents);
        }
    }

    @AutoValue
    abstract class RemoveDocumentsAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract ImmutableSet<Document> documents();

        static RemoveDocumentsAction create(@Nullable File containerFile,
                                            @Nullable ImmutableSet<Document> documents) {
            return new AutoValue_Action_RemoveDocumentsAction(containerFile, documents);
        }
    }

    @AutoValue
    abstract class SignatureListVisibilityAction implements Action {

        abstract boolean isVisible();

        static SignatureListVisibilityAction create(boolean isVisible) {
            return new AutoValue_Action_SignatureListVisibilityAction(isVisible);
        }
    }

    @AutoValue
    abstract class SignatureRemoveSelectionAction implements Action {

        @Nullable abstract Signature signature();

        static SignatureRemoveSelectionAction create(@Nullable Signature signature) {
            return new AutoValue_Action_SignatureRemoveSelectionAction(signature);
        }
    }

    @AutoValue
    abstract class SignatureRemoveAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract Signature signature();

        static SignatureRemoveAction create(@Nullable File containerFile,
                                            @Nullable Signature signature) {
            return new AutoValue_Action_SignatureRemoveAction(containerFile, signature);
        }
    }
}
