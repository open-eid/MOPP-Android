package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.signature.data.Signature;

final class SignatureAdapter extends RecyclerView.Adapter<SignatureAdapter.SignatureViewHolder> {

    private ImmutableList<Signature> signatures = ImmutableList.of();

    void setSignatures(ImmutableList<Signature> signatures) {
        DiffUtil.DiffResult diffResult = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.signatures, signatures));
        this.signatures = signatures;
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public SignatureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SignatureViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.signature_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(SignatureViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        Signature signature = signatures.get(position);

        holder.nameView.setText(signature.name());
        holder.createdAtView.setText(signature.createdAt().toString());
        if (signature.valid()) {
            holder.validityView.setImageResource(R.drawable.ic_check_circle);
            holder.validityView.setContentDescription(context.getString(
                    R.string.signature_update_signature_valid));
        } else {
            holder.validityView.setImageResource(R.drawable.ic_error);
            holder.validityView.setContentDescription(context.getString(
                    R.string.signature_update_signature_invalid));
        }
    }

    @Override
    public int getItemCount() {
        return signatures.size();
    }

    static final class SignatureViewHolder extends RecyclerView.ViewHolder {

        final TextView nameView;
        final TextView createdAtView;
        final ImageView validityView;

        SignatureViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureUpdateSignatureName);
            createdAtView = itemView.findViewById(R.id.signatureUpdateSignatureCreatedAt);
            validityView = itemView.findViewById(R.id.signatureUpdateSignatureValidity);
        }
    }

    static final class DiffUtilCallback extends DiffUtil.Callback {

        private final ImmutableList<Signature> oldList;
        private final ImmutableList<Signature> newList;

        DiffUtilCallback(ImmutableList<Signature> oldList, ImmutableList<Signature> newList) {
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
            return oldList.get(oldItemPosition).id().equals(newList.get(newItemPosition).id());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
