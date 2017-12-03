package ee.ria.DigiDoc.android.signature.container;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ChooseDocumentsResult implements Result {

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .chooseDocuments(true)
                    .build();
        }

        static ChooseDocumentsResult create() {
            return new AutoValue_Result_ChooseDocumentsResult();
        }
    }

    @AutoValue
    abstract class AddDocumentsResult implements Result {

        abstract boolean adding();

        @Nullable abstract ImmutableList<Document> documents();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .chooseDocuments(false)
                    .addingDocuments(adding())
                    .documents(documents())
                    .addDocumentsFailure(error())
                    .build();
        }

        static AddDocumentsResult inProgress() {
            return new AutoValue_Result_AddDocumentsResult(true, null, null);
        }

        static AddDocumentsResult success(ImmutableList<Document> documents) {
            return new AutoValue_Result_AddDocumentsResult(false, documents, null);
        }

        static AddDocumentsResult failure(Throwable error) {
            return new AutoValue_Result_AddDocumentsResult(false, null, error);
        }
    }
}
