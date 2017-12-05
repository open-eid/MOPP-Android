package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class LoadContainerResult implements Result {

        abstract boolean inProgress();

        @Nullable abstract SignatureContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state
                    .buildWith()
                    .loadContainerInProgress(inProgress())
                    .container(container())
                    .loadContainerError(error())
                    .build();
        }

        static LoadContainerResult progress() {
            return new AutoValue_Result_LoadContainerResult(true, null, null);
        }

        static LoadContainerResult success(SignatureContainer container) {
            return new AutoValue_Result_LoadContainerResult(false, container, null);
        }

        static LoadContainerResult failure(Throwable error) {
            return new AutoValue_Result_LoadContainerResult(false, null, error);
        }
    }

    @AutoValue
    abstract class AddDocumentsResult implements Result {

        abstract boolean isPicking();

        abstract boolean isAdding();

        @Nullable abstract SignatureContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .pickingDocuments(isPicking())
                    .documentsProgress(isAdding())
                    .addDocumentsError(error());
            if (container() != null) {
                builder.container(container());
            }
            return builder.build();
        }

        static AddDocumentsResult picking() {
            return new AutoValue_Result_AddDocumentsResult(true, false, null, null);
        }

        static AddDocumentsResult adding() {
            return new AutoValue_Result_AddDocumentsResult(false, true, null, null);
        }

        static AddDocumentsResult success(SignatureContainer container) {
            return new AutoValue_Result_AddDocumentsResult(false, false, container, null);
        }

        static AddDocumentsResult failure(Throwable error) {
            return new AutoValue_Result_AddDocumentsResult(false, false, null, error);
        }

        static AddDocumentsResult clear() {
            return new AutoValue_Result_AddDocumentsResult(false, false, null, null);
        }
    }

    @AutoValue
    abstract class OpenDocumentResult implements Result {

        abstract boolean isOpening();

        @Nullable abstract File documentFile();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .documentsProgress(isOpening())
                    .openedDocumentFile(documentFile())
                    .openDocumentError(error())
                    .build();
        }

        static OpenDocumentResult opening() {
            return new AutoValue_Result_OpenDocumentResult(true, null, null);
        }

        static OpenDocumentResult success(File documentFile) {
            return new AutoValue_Result_OpenDocumentResult(false, documentFile, null);
        }

        static OpenDocumentResult failure(Throwable error) {
            return new AutoValue_Result_OpenDocumentResult(false, null, error);
        }

        static OpenDocumentResult clear() {
            return new AutoValue_Result_OpenDocumentResult(false, null, null);
        }
    }
}
