package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.utils.Formatter;

final class SignatureAdapter extends RecyclerView.Adapter<SignatureAdapter.SignatureViewHolder> {

    private final ColorStateList colorValid;
    private final ColorStateList colorInvalid;
    private final Formatter formatter;

    private ImmutableList<Signature> signatures = ImmutableList.of();

    SignatureAdapter(Context context) {
        Resources resources = context.getResources();
        colorValid = ColorStateList.valueOf(resources.getColor(R.color.success));
        colorInvalid = ColorStateList.valueOf(resources.getColor(R.color.error));
        formatter = Application.component(context).formatter();
    }

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
        Resources resources = holder.itemView.getResources();

        Signature signature = signatures.get(position);

        holder.nameView.setText(signature.name());
        holder.createdAtView.setText(formatter.instant(signature.createdAt()));
        if (signature.valid()) {
            holder.validityView.setImageResource(R.drawable.ic_check_circle);
            holder.validityView.setContentDescription(resources.getString(
                    R.string.signature_update_signature_valid));
            holder.validityView.setImageTintList(colorValid);
        } else {
            holder.validityView.setImageResource(R.drawable.ic_error);
            holder.validityView.setContentDescription(resources.getString(
                    R.string.signature_update_signature_invalid));
            holder.validityView.setImageTintList(colorInvalid);
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
