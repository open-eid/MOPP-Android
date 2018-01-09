package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.navigation.NavigatorResult;
import ee.ria.DigiDoc.android.utils.navigation.Transaction;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;

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

    @AutoValue
    abstract class DocumentRemoveResult implements Result {

        @Nullable abstract Document showConfirmation();

        abstract boolean inProgress();

        @Nullable abstract SignatureContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .documentRemoveConfirmation(showConfirmation())
                    .documentRemoveInProgress(inProgress())
                    .documentRemoveError(error());
            if (container() != null) {
                builder.container(container());
            }
            return builder.build();
        }

        static DocumentRemoveResult confirmation(Document document) {
            return new AutoValue_Result_DocumentRemoveResult(document, false, null, null);
        }

        static DocumentRemoveResult progress() {
            return new AutoValue_Result_DocumentRemoveResult(null, true, null, null);
        }

        static DocumentRemoveResult success(SignatureContainer container) {
            return new AutoValue_Result_DocumentRemoveResult(null, false, container, null);
        }

        static DocumentRemoveResult failure(Throwable error) {
            return new AutoValue_Result_DocumentRemoveResult(null, false, null, error);
        }

        static DocumentRemoveResult clear() {
            return new AutoValue_Result_DocumentRemoveResult(null, false, null, null);
        }
    }

    @AutoValue
    abstract class SignatureRemoveResult implements Result {

        @Nullable abstract Signature showConfirmation();

        abstract boolean inProgress();

        @Nullable abstract SignatureContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .signatureRemoveConfirmation(showConfirmation())
                    .signatureRemoveInProgress(inProgress())
                    .signatureRemoveError(error());
            if (container() != null) {
                builder.container(container());
            }
            return builder.build();
        }

        static SignatureRemoveResult confirmation(Signature signature) {
            return new AutoValue_Result_SignatureRemoveResult(signature, false, null, null);
        }

        static SignatureRemoveResult progress() {
            return new AutoValue_Result_SignatureRemoveResult(null, true, null, null);
        }

        static SignatureRemoveResult success(SignatureContainer container) {
            return new AutoValue_Result_SignatureRemoveResult(null, false, container, null);
        }

        static SignatureRemoveResult failure(Throwable error) {
            return new AutoValue_Result_SignatureRemoveResult(null, false, null, error);
        }

        static SignatureRemoveResult clear() {
            return new AutoValue_Result_SignatureRemoveResult(null, false, null, null);
        }
    }

    @AutoValue
    abstract class SignatureAddResult implements Result, NavigatorResult {

        abstract boolean isCreatingContainer();

        abstract boolean isVisible();

        abstract boolean inProgress();

        @Nullable abstract GetMobileCreateSignatureStatusResponse.ProcessStatus status();

        @Nullable abstract String challenge();

        @Nullable abstract String signature();

        @Nullable abstract SignatureContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .signatureAddCreateContainerInProgress(isCreatingContainer())
                    .signatureAddVisible(isVisible())
                    .signatureAddInProgress(inProgress())
                    .signatureAddError(error());
            if (container() != null) {
                builder.signatureAddSuccessMessageVisible(true)
                        .container(container());
            } else {
                builder.signatureAddSuccessMessageVisible(false);
            }
            if (status() != null) {
                builder.signatureAddStatus(status());
            }
            if (challenge() != null) {
                builder.signatureAddChallenge(challenge());
            } else if (status() == null) {
                builder.signatureAddChallenge(null);
            }
            return builder.build();
        }

        static SignatureAddResult show() {
            return new AutoValue_Result_SignatureAddResult(null, false, true, false, null, null,
                    null, null, null);
        }

        static SignatureAddResult creatingContainer() {
            return new AutoValue_Result_SignatureAddResult(null, true, false, false, null, null,
                    null, null, null);
        }

        static SignatureAddResult status(
                GetMobileCreateSignatureStatusResponse.ProcessStatus status) {
            return new AutoValue_Result_SignatureAddResult(null, false, false, true, status, null,
                    null, null, null);
        }

        static SignatureAddResult challenge(String challenge) {
            return new AutoValue_Result_SignatureAddResult(null, false, false, true, null,
                    challenge, null, null, null);
        }

        static SignatureAddResult signature(String signature) {
            return new AutoValue_Result_SignatureAddResult(null, false, false, true,
                    GetMobileCreateSignatureStatusResponse.ProcessStatus.SIGNATURE, null, signature,
                    null, null);
        }

        static SignatureAddResult success(SignatureContainer container) {
            return new AutoValue_Result_SignatureAddResult(null, false, false, false, null, null,
                    null, container, null);
        }

        static SignatureAddResult failure(Throwable error) {
            return new AutoValue_Result_SignatureAddResult(null, false, false, false, null, null,
                    null, null, error);
        }

        static SignatureAddResult transaction(Transaction transaction) {
            return new AutoValue_Result_SignatureAddResult(transaction, false, false, false, null,
                    null, null, null, null);
        }

        static SignatureAddResult clear() {
            return new AutoValue_Result_SignatureAddResult(null, false, false, false, null, null,
                    null, null, null);
        }
    }
}
