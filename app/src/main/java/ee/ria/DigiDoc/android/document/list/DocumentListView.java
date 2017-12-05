package ee.ria.DigiDoc.android.document.list;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class DocumentListView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final DocumentListAdapter adapter;

    private final Navigator navigator;

    private final ViewDisposables disposables = new ViewDisposables();

    public DocumentListView(Context context) {
        this(context, null);
    }

    public DocumentListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DocumentListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.document_list, this);
        toolbarView = findViewById(R.id.toolbar);
        RecyclerView recyclerView = findViewById(R.id.documentListRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter = new DocumentListAdapter());

        navigator = Application.component(context).navigator();
    }

    public void setDocuments(ImmutableList<Document> documents) {
        adapter.setDocuments(documents);
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
