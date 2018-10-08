package ee.ria.DigiDoc.android.signature.list;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.R;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

final class SignatureListAdapter extends
        RecyclerView.Adapter<SignatureListAdapter.SignatureViewHolder> {

    private final Subject<File> itemClickSubject = PublishSubject.create();
    private final Subject<File> removeButtonClickSubject = PublishSubject.create();

    private ImmutableList<File> data = ImmutableList.of();

    void setData(ImmutableList<File> data) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtilCallback(this.data, data));
        this.data = data;
        result.dispatchUpdatesTo(this);
    }

    Observable<File> itemClicks() {
        return itemClickSubject;
    }

    Observable<File> removeButtonClicks() {
        return removeButtonClickSubject;
    }

    @Override
    public SignatureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SignatureViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.signature_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(SignatureViewHolder holder, int position) {
        holder.nameView.setText(data.get(position).getName());
        clicks(holder.itemView)
                .map(ignored -> data.get(holder.getAdapterPosition()))
                .subscribe(itemClickSubject);
        clicks(holder.removeButton)
                .map(ignored -> data.get(holder.getAdapterPosition()))
                .subscribe(removeButtonClickSubject);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static final class SignatureViewHolder extends RecyclerView.ViewHolder {

        final TextView nameView;
        final ImageButton removeButton;

        SignatureViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureListItemName);
            removeButton = itemView.findViewById(R.id.signatureListItemRemoveButton);
        }
    }

    static final class DiffUtilCallback extends DiffUtil.Callback {

        private final ImmutableList<File> oldList;
        private final ImmutableList<File> newList;

        DiffUtilCallback(ImmutableList<File> oldList, ImmutableList<File> newList) {
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
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
