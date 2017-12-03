package ee.ria.DigiDoc.android.signature.container;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract boolean chooseDocuments();

    abstract boolean addingDocuments();

    @Nullable abstract ImmutableList<Document> documents();

    @Nullable abstract Throwable addDocumentsFailure();

    abstract Builder buildWith();

    static ViewState idle() {
        return new AutoValue_ViewState.Builder()
                .chooseDocuments(false)
                .addingDocuments(false)
                .documents(null)
                .addDocumentsFailure(null)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder chooseDocuments(boolean chooseDocuments);
        Builder addingDocuments(boolean addingDocuments);
        Builder documents(@Nullable ImmutableList<Document> documents);
        Builder addDocumentsFailure(@Nullable Throwable addDocumentsFailure);
        ViewState build();
    }
}
