package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;

@AutoValue
abstract class ViewState implements MviViewState {

    @Nullable abstract SignedContainer container();
    abstract boolean containerLoadInProgress();
    @Nullable abstract Throwable containerLoadError();

    abstract boolean pickingDocuments();
    abstract boolean documentsProgress();
    @Nullable abstract Throwable documentsAddError();
    @Nullable abstract File documentOpenFile();
    @Nullable abstract Throwable documentOpenError();

    @Nullable abstract DataFile documentRemoveConfirmation();
    abstract boolean documentRemoveInProgress();
    @Nullable abstract Throwable documentRemoveError();

    @Nullable abstract Signature signatureRemoveConfirmation();
    abstract boolean signatureRemoveInProgress();
    @Nullable abstract Throwable signatureRemoveError();

    abstract boolean signatureAddCreateContainerInProgress();
    abstract boolean signatureAddVisible();
    abstract boolean signatureAddInProgress();
    @Nullable abstract GetMobileCreateSignatureStatusResponse.ProcessStatus signatureAddStatus();
    @Nullable abstract String signatureAddChallenge();
    abstract boolean signatureAddSuccessMessageVisible();
    @Nullable abstract Throwable signatureAddError();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .containerLoadInProgress(false)
                .pickingDocuments(false)
                .documentsProgress(false)
                .documentRemoveInProgress(false)
                .signatureRemoveInProgress(false)
                .signatureAddCreateContainerInProgress(false)
                .signatureAddVisible(false)
                .signatureAddInProgress(false)
                .signatureAddSuccessMessageVisible(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder container(@Nullable SignedContainer container);
        Builder containerLoadInProgress(boolean containerLoadInProgress);
        Builder containerLoadError(@Nullable Throwable containerLoadError);
        Builder pickingDocuments(boolean pickingDocuments);
        Builder documentsProgress(boolean documentsProgress);
        Builder documentsAddError(@Nullable Throwable documentsAddError);
        Builder documentOpenFile(@Nullable File documentOpenFile);
        Builder documentOpenError(@Nullable Throwable documentOpenError);
        Builder documentRemoveConfirmation(@Nullable DataFile documentRemoveConfirmation);
        Builder documentRemoveInProgress(boolean documentRemoveInProgress);
        Builder documentRemoveError(@Nullable Throwable documentRemoveError);
        Builder signatureRemoveConfirmation(@Nullable Signature signatureRemoveConfirmation);
        Builder signatureRemoveInProgress(boolean signatureRemoveInProgress);
        Builder signatureRemoveError(@Nullable Throwable signatureRemoveError);
        Builder signatureAddCreateContainerInProgress(
                boolean signatureAddCreateContainerInProgress);
        Builder signatureAddVisible(boolean signatureAddVisible);
        Builder signatureAddInProgress(boolean signatureAddInProgress);
        Builder signatureAddStatus(
                @Nullable GetMobileCreateSignatureStatusResponse.ProcessStatus signatureAddStatus);
        Builder signatureAddChallenge(@Nullable String signatureAddChallenge);
        Builder signatureAddSuccessMessageVisible(boolean signatureAddSuccessMessageVisible);
        Builder signatureAddError(@Nullable Throwable signatureAddError);
        ViewState build();
    }
}
