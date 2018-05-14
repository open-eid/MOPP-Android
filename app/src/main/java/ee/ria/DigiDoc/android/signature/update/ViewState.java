package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;

@AutoValue
abstract class ViewState implements MviViewState {

    @Nullable abstract SignedContainer container();
    abstract boolean containerLoadInProgress();
    @Nullable abstract Throwable containerLoadError();

    abstract boolean nameUpdateShowing();
    @Nullable abstract String nameUpdateName();
    abstract boolean nameUpdateInProgress();
    @Nullable abstract Throwable nameUpdateError();

    abstract boolean documentsAddInProgress();
    @Nullable abstract Throwable documentsAddError();

    @Nullable abstract File documentOpenFile();
    abstract boolean documentOpenInProgress();
    @Nullable abstract Throwable documentOpenError();

    @Nullable abstract DataFile documentRemoveConfirmation();
    abstract boolean documentRemoveInProgress();
    @Nullable abstract Throwable documentRemoveError();

    @Nullable abstract Signature signatureRemoveConfirmation();
    abstract boolean signatureRemoveInProgress();
    @Nullable abstract Throwable signatureRemoveError();

    @Nullable abstract Integer signatureAddMethod();
    abstract boolean signatureAddActivity();
    abstract boolean signatureAddSuccessMessageVisible();
    @Nullable abstract Throwable signatureAddError();
    @Nullable abstract SignatureAddResponse signatureAddResponse();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .containerLoadInProgress(false)
                .nameUpdateShowing(false)
                .nameUpdateInProgress(false)
                .documentsAddInProgress(false)
                .documentOpenInProgress(false)
                .documentRemoveInProgress(false)
                .signatureRemoveInProgress(false)
                .signatureAddActivity(false)
                .signatureAddSuccessMessageVisible(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder container(@Nullable SignedContainer container);
        Builder containerLoadInProgress(boolean containerLoadInProgress);
        Builder containerLoadError(@Nullable Throwable containerLoadError);
        Builder nameUpdateShowing(boolean nameUpdateShowing);
        Builder nameUpdateName(@Nullable String nameUpdateName);
        Builder nameUpdateInProgress(boolean nameUpdateInProgress);
        Builder nameUpdateError(@Nullable Throwable nameUpdateError);
        Builder documentsAddInProgress(boolean documentsAddInProgress);
        Builder documentsAddError(@Nullable Throwable documentsAddError);
        Builder documentOpenFile(@Nullable File documentOpenFile);
        Builder documentOpenInProgress(boolean documentOpenInProgress);
        Builder documentOpenError(@Nullable Throwable documentOpenError);
        Builder documentRemoveConfirmation(@Nullable DataFile documentRemoveConfirmation);
        Builder documentRemoveInProgress(boolean documentRemoveInProgress);
        Builder documentRemoveError(@Nullable Throwable documentRemoveError);
        Builder signatureRemoveConfirmation(@Nullable Signature signatureRemoveConfirmation);
        Builder signatureRemoveInProgress(boolean signatureRemoveInProgress);
        Builder signatureRemoveError(@Nullable Throwable signatureRemoveError);
        Builder signatureAddMethod(@Nullable Integer signatureAddMethod);
        Builder signatureAddActivity(boolean signatureAddActivity);
        Builder signatureAddSuccessMessageVisible(boolean signatureAddSuccessMessageVisible);
        Builder signatureAddError(@Nullable Throwable signatureAddError);
        Builder signatureAddResponse(@Nullable SignatureAddResponse signatureAddResponse);
        ViewState build();
    }
}
