package ee.ria.DigiDoc.android.document.list;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.document.data.Document;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

/**
 * Document list that is shown in signature or crypto containers.
 */
public final class DocumentListContainerView extends CardView {

    private final View progressView;
    private final Button expandButton;
    private final Button addButton;
    private final DocumentListAdapter adapter;

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
        progressView = findViewById(android.R.id.secondaryProgress);
        RecyclerView recyclerView = findViewById(R.id.documentListRecycler);
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
    }

    public Observable<Document> documentClicks() {
        return adapter.itemClicks();
    }

    public Observable<Document> documentLongClicks() {
        return adapter.itemLongClicks();
    }

    public Observable<Object> expandButtonClicks() {
        return clicks(expandButton);
    }

    public Observable<Object> addButtonClicks() {
        return clicks(addButton);
    }

    public void setAddButtonVisible(boolean addButtonVisible) {
        addButton.setVisibility(addButtonVisible ? VISIBLE : GONE);
    }

    public void setProgress(boolean progress) {
        progressView.setVisibility(progress ? VISIBLE : GONE);
    }

    public boolean isEmpty() {
        return adapter.getDocuments().size() == 0;
    }

    public ImmutableList<Document> getDocuments() {
        return adapter.getDocuments();
    }

    public void setDocuments(ImmutableList<Document> documents) {
        adapter.setDocuments(documents);

        expandButton.setVisibility(adapter.getItemCount() > 0 ? VISIBLE : GONE);
        expandButton.setText(getResources().getString(R.string.document_list_expand_button,
                adapter.getItemCount()));
    }
}
