package ee.ria.DigiDoc.android.document.list;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.Button;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

/**
 * Document list that is shown in signature or crypto containers.
 */
public final class DocumentListContainerView extends CardView {

    private final RecyclerView recyclerView;
    private final Button expandButton;
    private final Button addButton;
    private final DocumentListAdapter adapter;

    private final Navigator navigator;

    private final ViewDisposables disposables = new ViewDisposables();

    public DocumentListContainerView(Context context) {
        this(context, null);
    }

    public DocumentListContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DocumentListContainerView(Context context, @Nullable AttributeSet attrs,
                                     int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.document_list_container, this);
        recyclerView = findViewById(R.id.documentListRecycler);
        expandButton = findViewById(R.id.documentListExpandButton);
        addButton = findViewById(R.id.documentListAddButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(context) {
            @Override
            public boolean canScrollVertically() {
                // disable scrolling inside card view
                return false;
            }
        });
        recyclerView.setAdapter(adapter = new DocumentListAdapter());

        navigator = Application.component(context).navigator();
    }

    public void setDocuments(ImmutableList<Document> documents) {
        adapter.setDocuments(documents);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(clicks(expandButton).subscribe(o ->
                navigator.pushScreen(DocumentListScreen.create(adapter.getDocuments()))));
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
