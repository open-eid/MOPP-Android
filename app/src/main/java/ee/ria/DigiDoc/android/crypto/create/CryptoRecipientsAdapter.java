package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.Formatter;

final class CryptoRecipientsAdapter extends
        RecyclerView.Adapter<CryptoRecipientsAdapter.RecipientsViewHolder> {

    private final Formatter formatter;

    private ImmutableList<Certificate> existing = ImmutableList.of();
    private ImmutableList<Certificate> result = ImmutableList.of();

    CryptoRecipientsAdapter(Formatter formatter) {
        this.formatter = formatter;
    }

    void setData(ImmutableList<Certificate> existing, ImmutableList<Certificate> result) {
        this.existing = existing;
        this.result = result;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipientsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecipientsViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.crypto_list_item_recipient, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecipientsViewHolder holder, int position) {
        Certificate certificate = result.get(position);

        holder.nameView.setText(certificate.commonName());
        holder.infoView.setText(holder.itemView.getResources().getString(
                R.string.crypto_recipient_info, formatter.eidType(certificate.type()),
                certificate.notAfter()));
        if (existing.contains(certificate)) {
            holder.addButton.setEnabled(false);
            holder.addButton.setText(R.string.crypto_recipient_add_button_added);
        } else {
            holder.addButton.setEnabled(true);
            holder.addButton.setText(R.string.crypto_recipient_add_button);
        }
    }

    @Override
    public int getItemCount() {
        return result.size();
    }

    static final class RecipientsViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView infoView;
        private final Button addButton;

        RecipientsViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.cryptoRecipientName);
            infoView = itemView.findViewById(R.id.cryptoRecipientInfo);
            addButton = itemView.findViewById(R.id.cryptoRecipientAddButton);
            itemView.findViewById(R.id.cryptoRecipientRemoveButton).setVisibility(View.GONE);
        }
    }
}
