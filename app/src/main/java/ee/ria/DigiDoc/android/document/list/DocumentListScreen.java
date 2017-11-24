package ee.ria.DigiDoc.android.document.list;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

import static ee.ria.DigiDoc.android.utils.BundleUtils.getParcelableImmutableList;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putParcelableImmutableList;

public final class DocumentListScreen extends ConductorScreen {

    private static final String DOCUMENTS = "documents";

    public static DocumentListScreen create(ImmutableList<Document> documents) {
        Bundle args = new Bundle();
        putParcelableImmutableList(args, DOCUMENTS, documents);
        return new DocumentListScreen(args);
    }

    private final ImmutableList<Document> documents;

    @SuppressWarnings("WeakerAccess")
    public DocumentListScreen(Bundle args) {
        super(R.id.documentListScreen, args);
        documents = getParcelableImmutableList(args, DOCUMENTS);
    }

    @Override
    protected View createView(Context context) {
        DocumentListView view = new DocumentListView(context);
        view.setDocuments(documents);
        return view;
    }
}
