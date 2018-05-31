package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.threeten.bp.LocalDate;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.EIDType;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.cryptolib.Recipient;

import static ee.ria.DigiDoc.android.crypto.create.CryptoRecipientsAdapter.RecipientButtonType.ADD;
import static ee.ria.DigiDoc.android.crypto.create.CryptoRecipientsAdapter.RecipientButtonType.ADDED;
import static ee.ria.DigiDoc.android.crypto.create.CryptoRecipientsAdapter.RecipientButtonType.REMOVE;

final class CryptoRecipientsAdapter extends
        RecyclerView.Adapter<
                CryptoRecipientsAdapter.RecipientsViewHolder<CryptoRecipientsAdapter.Item>> {

    private ImmutableList<Item> items = ImmutableList.of(
            RecipientItem.create(
                    Recipient.create("Mari Maasikas, 48405050123", EIDType.DIGI_ID,
                            LocalDate.now()),
                    RecipientButtonType.ADD),
            RecipientItem.create(
                    Recipient.create("Jüri Juurikas, 38405050123", EIDType.ID_CARD,
                            LocalDate.now()),
                    RecipientButtonType.ADDED),
            SubheadItem.create(R.string.crypto_recipients_selected_subhead),
            RecipientItem.create(
                    Recipient.create("Mari Maasikas, 48405050123", EIDType.DIGI_ID,
                            LocalDate.now()),
                    RecipientButtonType.REMOVE),
            RecipientItem.create(
                    Recipient.create("Jüri Juurikas, 38405050123", EIDType.ID_CARD,
                            LocalDate.now()),
                    RecipientButtonType.REMOVE));

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type();
    }

    @NonNull
    @Override
    public RecipientsViewHolder<Item> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //noinspection unchecked
        return RecipientsViewHolder.create(viewType,
                LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecipientsViewHolder<Item> holder, int position) {
        holder.bind(this, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static abstract class RecipientsViewHolder<T extends Item> extends RecyclerView.ViewHolder {

        RecipientsViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(CryptoRecipientsAdapter adapter, T item);

        static RecipientsViewHolder create(@LayoutRes int viewType, View itemView) {
            switch (viewType) {
                case R.layout.crypto_list_item_recipient:
                    return new RecipientViewHolder(itemView);
                case R.layout.crypto_list_item_subhead:
                    return new SubheadViewHolder(itemView);
                default:
                    throw new IllegalArgumentException("Unknown view type " + viewType);
            }
        }
    }

    static final class RecipientViewHolder extends RecipientsViewHolder<RecipientItem> {

        private final Formatter formatter;

        private final TextView nameView;
        private final TextView infoView;
        private final Button addButton;
        private final View removeButton;

        RecipientViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            nameView = itemView.findViewById(R.id.cryptoRecipientName);
            infoView = itemView.findViewById(R.id.cryptoRecipientInfo);
            addButton = itemView.findViewById(R.id.cryptoRecipientAddButton);
            removeButton = itemView.findViewById(R.id.cryptoRecipientRemoveButton);
        }

        @Override
        void bind(CryptoRecipientsAdapter adapter, RecipientItem item) {
            nameView.setText(item.recipient().name());
            infoView.setText(itemView.getResources().getString(
                    R.string.crypto_recipient_info,
                    formatter.eidType(item.recipient().type()), item.recipient().expiryDate()));
            switch (item.buttonType()) {
                case RecipientButtonType.ADD:
                    addButton.setVisibility(View.VISIBLE);
                    addButton.setEnabled(true);
                    addButton.setText(R.string.crypto_recipient_add_button);
                    removeButton.setVisibility(View.GONE);
                    break;
                case RecipientButtonType.ADDED:
                    addButton.setVisibility(View.VISIBLE);
                    addButton.setEnabled(false);
                    addButton.setText(R.string.crypto_recipient_add_button_added);
                    removeButton.setVisibility(View.GONE);
                    break;
                case RecipientButtonType.REMOVE:
                    addButton.setVisibility(View.GONE);
                    removeButton.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    static final class SubheadViewHolder extends RecipientsViewHolder<SubheadItem> {

        private final TextView titleView;

        SubheadViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.cryptoSubheadTitle);
        }

        @Override
        void bind(CryptoRecipientsAdapter adapter, SubheadItem item) {
            titleView.setText(item.title());
        }
    }

    static abstract class Item {

        @LayoutRes abstract int type();
    }

    @AutoValue
    static abstract class RecipientItem extends Item {

        abstract Recipient recipient();

        @RecipientButtonType abstract int buttonType();

        static RecipientItem create(Recipient recipient, @RecipientButtonType int buttonType) {
            return new AutoValue_CryptoRecipientsAdapter_RecipientItem(
                    R.layout.crypto_list_item_recipient, recipient, buttonType);
        }
    }

    @AutoValue
    static abstract class SubheadItem extends Item {

        @StringRes abstract int title();

        static SubheadItem create(@StringRes int title) {
            return new AutoValue_CryptoRecipientsAdapter_SubheadItem(
                    R.layout.crypto_list_item_subhead, title);
        }
    }

    @IntDef({ADD, ADDED, REMOVE})
    @interface RecipientButtonType {
        int ADD = 0;
        int ADDED = 1;
        int REMOVE = 2;
    }
}
