package ee.ria.DigiDoc.android.document.list;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.Button;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.document.data.Document;

/**
 * Document list that is shown in signature or crypto containers.
 */
public final class DocumentListContainerView extends CardView {

    private final RecyclerView recyclerView;
    private final Button expandButton;
    private final Button addButton;
    private final DocumentListAdapter adapter;

    public DocumentListContainerView(Context context) {
        this(context, null);
    }

    public DocumentListContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DocumentListContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
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
    }

    public void setDocuments(ImmutableList<Document> documents) {
        adapter.setDocuments(documents);
    }
}
