package ee.ria.DigiDoc.android.signature.container;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.document.list.DocumentListContainerView;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class SignatureContainerView extends CoordinatorLayout {

    private static final ImmutableList<Document> DOCUMENTS = ImmutableList.<Document>builder()
            .add(Document.create("Dokument 1.pdf"))
            .add(Document.create("Notari tähtis dokument.pdf"))
            .add(Document.create("Väga pika nimega leping mis kindlasti on vaja allkirjastada.docx"))
            .add(Document.create("Leping.pdf"))
            .add(Document.create("Tingimused.pdf"))
            .add(Document.create("Taotlus.pdf"))
            .add(Document.create("Arve 1123332.pdf"))
            .build();

    private final Toolbar toolbarView;
    private final DocumentListContainerView documentsView;

    private final Navigator navigator;

    private final ViewDisposables disposables = new ViewDisposables();

    public SignatureContainerView(Context context) {
        this(context, null);
    }

    public SignatureContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureContainerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_container, this);
        toolbarView = findViewById(R.id.toolbar);
        documentsView = findViewById(R.id.signatureContainerDocuments);

        documentsView.setDocuments(DOCUMENTS);

        navigator = Application.component(context).navigator();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o -> navigator.popScreen()));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
