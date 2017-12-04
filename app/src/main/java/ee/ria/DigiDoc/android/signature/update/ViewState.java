package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    @Nullable abstract SignatureContainer container();

    abstract boolean loadContainerInProgress();

    @Nullable abstract Throwable loadContainerError();

    abstract boolean pickingDocuments();

    abstract boolean addingDocuments();

    @Nullable abstract Throwable addDocumentsError();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .loadContainerInProgress(false)
                .pickingDocuments(false)
                .addingDocuments(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder container(@Nullable SignatureContainer container);
        Builder loadContainerInProgress(boolean loadContainerInProgress);
        Builder loadContainerError(@Nullable Throwable error);
        Builder pickingDocuments(boolean pickingDocuments);
        Builder addingDocuments(boolean addingDocuments);
        Builder addDocumentsError(@Nullable Throwable addDocumentsError);
        ViewState build();
    }
}
