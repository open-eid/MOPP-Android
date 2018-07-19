package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.sign.data.DataFile;
import ee.ria.DigiDoc.sign.data.Signature;
import ee.ria.DigiDoc.sign.data.SignedContainer;

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

    @State abstract String documentViewState();

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
                .documentViewState(State.IDLE)
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
        Builder documentViewState(@State String documentViewState);
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
