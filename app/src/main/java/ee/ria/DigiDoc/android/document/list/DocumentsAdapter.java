package ee.ria.DigiDoc.android.document.list;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ee.ria.DigiDoc.android.document.data.Document;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class DocumentsAdapter extends
        RecyclerView.Adapter<DocumentsAdapter.DocumentsViewHolder> {

    private final Subject<Document> clicksSubject = PublishSubject.create();
    private final Subject<Document> longClicksSubject = PublishSubject.create();

    private ImmutableList<SelectableItem<Document>> documents = ImmutableList.of();

    public void setDocuments(ImmutableList<Document> documents,
                             @Nullable ImmutableSet<Document> selected) {
        ImmutableList.Builder<SelectableItem<Document>> itemsBuilder = ImmutableList.builder();
        for (Document document : documents) {
            itemsBuilder.add(SelectableItem.create(document,
                    selected != null && selected.contains(document)));
        }
        ImmutableList<SelectableItem<Document>> items = itemsBuilder.build();

        DiffUtil.DiffResult diffResult = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.documents, items));
        this.documents = items;
        diffResult.dispatchUpdatesTo(this);
    }

    public Observable<Document> itemClicks() {
        return clicksSubject;
    }

    public Observable<Document> itemLongClicks() {
        return longClicksSubject;
    }

    @Override
    public DocumentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DocumentsViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_activated_1, parent, false));
    }

    @Override
    public void onBindViewHolder(DocumentsViewHolder holder, int position) {
        SelectableItem<Document> item = documents.get(position);

        holder.itemView.setActivated(item.selected());
        holder.textView.setText(item.value().name());

        holder.itemView.setOnClickListener(ignored ->
                clicksSubject.onNext(documents.get(holder.getAdapterPosition()).value()));
        holder.itemView.setOnLongClickListener(ignored -> {
            longClicksSubject.onNext(documents.get(holder.getAdapterPosition()).value());
            return true;
        });
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

        private final ImmutableList<SelectableItem<Document>> oldList;
        private final ImmutableList<SelectableItem<Document>> newList;

        DiffUtilCallback(ImmutableList<SelectableItem<Document>> oldList,
                         ImmutableList<SelectableItem<Document>> newList) {
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
            return oldList.get(oldItemPosition).value().name()
                    .equals(newList.get(newItemPosition).value().name());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    @AutoValue
    static abstract class SelectableItem<T> {

        abstract T value();

        abstract boolean selected();

        static <T> SelectableItem<T> create(T value, boolean selected) {
            return new AutoValue_DocumentsAdapter_SelectableItem<>(value, selected);
        }
    }
}
