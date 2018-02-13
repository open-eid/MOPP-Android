package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ContainerLoadResult implements Result {

        abstract boolean inProgress();

        @Nullable abstract SignedContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state
                    .buildWith()
                    .containerLoadInProgress(inProgress())
                    .container(container())
                    .containerLoadError(error())
                    .build();
        }

        static ContainerLoadResult progress() {
            return new AutoValue_Result_ContainerLoadResult(true, null, null);
        }

        static ContainerLoadResult success(SignedContainer container) {
            return new AutoValue_Result_ContainerLoadResult(false, container, null);
        }

        static ContainerLoadResult failure(Throwable error) {
            return new AutoValue_Result_ContainerLoadResult(false, null, error);
        }
    }

    @AutoValue
    abstract class DocumentsAddResult implements Result {

        abstract boolean inProgress();

        @Nullable abstract SignedContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .documentsAddInProgress(inProgress())
                    .documentsAddError(error());
            if (container() != null) {
                builder.container(container());
            }
            return builder.build();
        }

        static DocumentsAddResult adding() {
            return new AutoValue_Result_DocumentsAddResult(true, null, null);
        }

        static DocumentsAddResult success(SignedContainer container) {
            return new AutoValue_Result_DocumentsAddResult(false, container, null);
        }

        static DocumentsAddResult failure(Throwable error) {
            return new AutoValue_Result_DocumentsAddResult(false, null, error);
        }

        static DocumentsAddResult clear() {
            return new AutoValue_Result_DocumentsAddResult(false, null, null);
        }
    }

    @AutoValue
    abstract class DocumentOpenResult implements Result {

        abstract boolean isOpening();

        @Nullable abstract File documentFile();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .documentOpenInProgress(isOpening())
                    .documentOpenFile(documentFile())
                    .documentOpenError(error())
                    .build();
        }

        static DocumentOpenResult opening() {
            return new AutoValue_Result_DocumentOpenResult(true, null, null);
        }

        static DocumentOpenResult success(File documentFile) {
            return new AutoValue_Result_DocumentOpenResult(false, documentFile, null);
        }

        static DocumentOpenResult failure(Throwable error) {
            return new AutoValue_Result_DocumentOpenResult(false, null, error);
        }

        static DocumentOpenResult clear() {
            return new AutoValue_Result_DocumentOpenResult(false, null, null);
        }
    }

    @AutoValue
    abstract class DocumentRemoveResult implements Result {

        @Nullable abstract DataFile showConfirmation();

        abstract boolean inProgress();

        @Nullable abstract SignedContainer container();

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

        static DocumentRemoveResult confirmation(DataFile document) {
            return new AutoValue_Result_DocumentRemoveResult(document, false, null, null);
        }

        static DocumentRemoveResult progress() {
            return new AutoValue_Result_DocumentRemoveResult(null, true, null, null);
        }

        static DocumentRemoveResult success(SignedContainer container) {
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

        @Nullable abstract SignedContainer container();

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

        static SignatureRemoveResult success(SignedContainer container) {
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
    abstract class SignatureAddResult implements Result {

        abstract boolean isCreatingContainer();

        abstract boolean isVisible();

        abstract boolean inProgress();

        @Nullable abstract GetMobileCreateSignatureStatusResponse.ProcessStatus status();

        @Nullable abstract String challenge();

        @Nullable abstract String signature();

        @Nullable abstract SignedContainer container();

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
            return new AutoValue_Result_SignatureAddResult(false, true, false, null, null, null,
                    null, null);
        }

        static SignatureAddResult creatingContainer() {
            return new AutoValue_Result_SignatureAddResult(true, false, false, null, null, null,
                    null, null);
        }

        static SignatureAddResult status(
                GetMobileCreateSignatureStatusResponse.ProcessStatus status) {
            return new AutoValue_Result_SignatureAddResult(false, false, true, status, null, null,
                    null, null);
        }

        static SignatureAddResult challenge(String challenge) {
            return new AutoValue_Result_SignatureAddResult(false, false, true, null, challenge,
                    null, null, null);
        }

        static SignatureAddResult signature(String signature) {
            return new AutoValue_Result_SignatureAddResult(false, false, true,
                    GetMobileCreateSignatureStatusResponse.ProcessStatus.SIGNATURE, null, signature,
                    null, null);
        }

        static SignatureAddResult success(SignedContainer container) {
            return new AutoValue_Result_SignatureAddResult(false, false, false, null, null, null,
                    container, null);
        }

        static SignatureAddResult failure(Throwable error) {
            return new AutoValue_Result_SignatureAddResult(false, false, false, null, null, null,
                    null, error);
        }

        static SignatureAddResult clear() {
            return new AutoValue_Result_SignatureAddResult(false, false, false, null, null, null,
                    null, null);
        }
    }

    @AutoValue
    abstract class SendResult implements Result {

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static SendResult success() {
            return new AutoValue_Result_SendResult(null);
        }

        static SendResult failure(Throwable error) {
            return new AutoValue_Result_SendResult(error);
        }
    }
}
