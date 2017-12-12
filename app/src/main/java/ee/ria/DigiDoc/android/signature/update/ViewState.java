package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    @Nullable abstract SignatureContainer container();

    abstract boolean loadContainerInProgress();

    @Nullable abstract Throwable loadContainerError();

    abstract boolean pickingDocuments();

    abstract boolean documentsProgress();

    @Nullable abstract Throwable addDocumentsError();

    @Nullable abstract File openedDocumentFile();

    @Nullable abstract Throwable openDocumentError();

    @Nullable abstract ImmutableSet<Document> selectedDocuments();

    @Nullable abstract Throwable removeDocumentsError();

    abstract boolean signatureListVisible();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .loadContainerInProgress(false)
                .pickingDocuments(false)
                .documentsProgress(false)
                .signatureListVisible(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder container(@Nullable SignatureContainer container);
        Builder loadContainerInProgress(boolean loadContainerInProgress);
        Builder loadContainerError(@Nullable Throwable error);
        Builder pickingDocuments(boolean pickingDocuments);
        Builder documentsProgress(boolean documentsProgress);
        Builder addDocumentsError(@Nullable Throwable addDocumentsError);
        Builder openedDocumentFile(@Nullable File openedDocumentFile);
        Builder openDocumentError(@Nullable Throwable openDocumentError);
        Builder selectedDocuments(ImmutableSet<Document> selectedDocuments);
        Builder removeDocumentsError(@Nullable Throwable removeDocumentsError);
        Builder signatureListVisible(boolean signatureListVisible);
        ViewState build();
    }
}
