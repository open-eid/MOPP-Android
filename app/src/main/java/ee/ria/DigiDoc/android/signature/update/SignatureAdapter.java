package ee.ria.DigiDoc.android.signature.update;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ee.ria.DigiDoc.R;

final class SignatureAdapter extends RecyclerView.Adapter<SignatureAdapter.SignatureViewHolder> {

    @Override
    public SignatureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SignatureViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.signature_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(SignatureViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    static final class SignatureViewHolder extends RecyclerView.ViewHolder {

        final TextView nameView;
        final TextView createdAtView;
        final ImageView indicatorView;

        SignatureViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureUpdateSignatureName);
            createdAtView = itemView.findViewById(R.id.signatureUpdateSignatureCreatedAt);
            indicatorView = itemView.findViewById(R.id.signatureUpdateSignatureValidity);
        }
    }
}
