package ee.ria.DigiDoc.android.signature.container;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class SignatureContainerViewState implements MviViewState {

    abstract String name();

    abstract ImmutableList<Document> documents();

    abstract boolean documentsAddProgress();

    abstract Throwable documentsAddFailure();
}
