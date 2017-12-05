package ee.ria.DigiDoc.android.document.list;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;

final class DocumentListAdapter
        extends RecyclerView.Adapter<DocumentListAdapter.DocumentsViewHolder> {

    private ImmutableList<Document> documents = ImmutableList.of();

    ImmutableList<Document> getDocuments() {
        return documents;
    }

    void setDocuments(ImmutableList<Document> documents) {
        DiffUtil.DiffResult diffResult = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.documents, documents));
        this.documents = documents;
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public DocumentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DocumentsViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    @Override
    public void onBindViewHolder(DocumentsViewHolder holder, int position) {
        holder.textView.setText(documents.get(position).name());
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static final class DocumentsViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        DocumentsViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }

    static final class DiffUtilCallback extends DiffUtil.Callback {

        private final ImmutableList<Document> oldList;
        private final ImmutableList<Document> newList;

        DiffUtilCallback(ImmutableList<Document> oldList, ImmutableList<Document> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).name().equals(newList.get(newItemPosition).name());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
