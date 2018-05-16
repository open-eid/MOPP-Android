package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ContainerLoadResult implements Result {

        abstract boolean inProgress();

        @Nullable abstract SignedContainer container();

        @Nullable abstract Integer signatureAddMethod();

        abstract boolean signatureAddSuccessMessageVisible();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state
                    .buildWith()
                    .containerLoadInProgress(inProgress())
                    .container(container())
                    .containerLoadError(error())
                    .signatureAddMethod(signatureAddMethod())
                    .signatureAddSuccessMessageVisible(signatureAddSuccessMessageVisible())
                    .build();
        }

        static ContainerLoadResult progress() {
            return new AutoValue_Result_ContainerLoadResult(true, null, null, false, null);
        }

        static ContainerLoadResult success(SignedContainer container,
                                           @Nullable Integer signatureAddMethod,
                                           boolean signatureAddSuccessMessageVisible) {
            return new AutoValue_Result_ContainerLoadResult(false, container, signatureAddMethod,
                    signatureAddSuccessMessageVisible, null);
        }

        static ContainerLoadResult failure(Throwable error) {
            return new AutoValue_Result_ContainerLoadResult(false, null, null, false, error);
        }
    }

    @AutoValue
    abstract class NameUpdateResult implements Result {

        @Nullable abstract File containerFile();

        @Nullable abstract String name();

        abstract boolean inProgress();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .nameUpdateShowing(containerFile() != null)
                    .nameUpdateName(name())
                    .nameUpdateInProgress(inProgress())
                    .nameUpdateError(error())
                    .build();
        }

        static NameUpdateResult name(File containerFile) {
            return create(containerFile, containerFile.getName(), false, null);
        }

        static NameUpdateResult show(File containerFile) {
            return create(containerFile, null, false, null);
        }

        static NameUpdateResult hide() {
            return create(null, null, false, null);
        }

        static NameUpdateResult progress(File containerFile) {
            return create(containerFile, null, true, null);
        }

        static NameUpdateResult failure(File containerFile, Throwable error) {
            return create(containerFile, null, false, error);
        }

        private static NameUpdateResult create(@Nullable File containerFile, @Nullable String name,
                                               boolean inProgress, @Nullable Throwable error) {
            return new AutoValue_Result_NameUpdateResult(containerFile, name, inProgress, error);
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

        @Nullable abstract Integer method();

        abstract boolean active();

        @Nullable abstract SignatureAddResponse response();

        @Nullable abstract SignedContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            SignatureAddResponse response = response();
            if (response != null) {
                response = response.mergeWith(state.signatureAddResponse());
            }
            ViewState.Builder builder = state.buildWith()
                    .signatureAddMethod(method())
                    .signatureAddActivity(active())
                    .signatureAddResponse(response)
                    .signatureAddError(error());
            if (container() != null) {
                builder.signatureAddSuccessMessageVisible(true)
                        .container(container());
            } else {
                builder.signatureAddSuccessMessageVisible(false);
            }
            return builder.build();
        }

        static SignatureAddResult show(int method) {
            return create(method, false, null, null, null);
        }

        static SignatureAddResult activity() {
            return create(null, true, null, null, null);
        }

        static SignatureAddResult activity(int method) {
            return create(method, true, null, null, null);
        }

        static SignatureAddResult method(int method, SignatureAddResponse response) {
            return create(response.showDialog() ? method : null, response.active(), response, null,
                    null);
        }

        static SignatureAddResult success(SignedContainer container) {
            return create(null, false, null, container, null);
        }

        static SignatureAddResult failure(Throwable error) {
            return create(null, false, null, null, error);
        }

        static SignatureAddResult clear() {
            return create(null, false, null, null, null);
        }

        private static SignatureAddResult create(@Nullable Integer method, boolean active,
                                                 @Nullable SignatureAddResponse response,
                                                 @Nullable SignedContainer container,
                                                 @Nullable Throwable error) {
            return new AutoValue_Result_SignatureAddResult(method, active, response, container,
                    error);
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
