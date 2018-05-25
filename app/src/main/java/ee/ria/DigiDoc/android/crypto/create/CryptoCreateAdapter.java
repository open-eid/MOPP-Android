package ee.ria.DigiDoc.android.crypto.create;

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

import ee.ria.DigiDoc.R;

public final class CryptoCreateAdapter extends
        RecyclerView.Adapter<CryptoCreateAdapter.CreateViewHolder<CryptoCreateAdapter.Item>> {

    private ImmutableList<Item> items = ImmutableList
            .of(SuccessItem.create(), NameItem.create("some_cool_name.cdoc"),
                    SubheadItem.create(R.string.crypto_create_data_files_title),
                    AddButtonItem.create(R.string.crypto_create_data_files_add_button),
                    SubheadItem.create(R.string.crypto_create_recipients_title),
                    AddButtonItem.create(R.string.crypto_create_recipients_add_button));

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type();
    }

    @NonNull
    @Override
    public CreateViewHolder<Item> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //noinspection unchecked
        return CreateViewHolder.create(viewType,
                LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CreateViewHolder<Item> holder, int position) {
        holder.bind(this, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static abstract class CreateViewHolder<T extends Item> extends RecyclerView.ViewHolder {

        CreateViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(CryptoCreateAdapter adapter, T item);

        static CreateViewHolder create(int viewType, View itemView) {
            switch (viewType) {
                case R.layout.crypto_create_list_item_success:
                    return new SuccessViewHolder(itemView);
                case R.layout.crypto_create_list_item_name:
                    return new NameViewHolder(itemView);
                case R.layout.crypto_create_list_item_subhead:
                    return new SubheadViewHolder(itemView);
                case R.layout.crypto_create_list_item_add_button:
                    return new AddButtonViewHolder(itemView);
                default:
                    throw new IllegalArgumentException("Unknown view type " + viewType);
            }
        }
    }

    static final class SuccessViewHolder extends CreateViewHolder<SuccessItem> {

        SuccessViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, SuccessItem item) {}
    }

    static final class NameViewHolder extends CreateViewHolder<NameItem> {

        private final TextView nameView;

        NameViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.cryptoCreateName);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, NameItem item) {
            nameView.setText(item.name());
        }
    }

    static final class SubheadViewHolder extends CreateViewHolder<SubheadItem> {

        private final TextView titleView;

        SubheadViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.cryptoCreateSubheadTitle);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, SubheadItem item) {
            titleView.setText(item.title());
        }
    }

    static final class AddButtonViewHolder extends CreateViewHolder<AddButtonItem> {

        private final Button buttonView;

        AddButtonViewHolder(View itemView) {
            super(itemView);
            buttonView = itemView.findViewById(R.id.cryptoCreateAddButton);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, AddButtonItem item) {
            buttonView.setText(item.text());
        }
    }

    static abstract class Item {

        @LayoutRes abstract int type();
    }

    @AutoValue
    static abstract class SuccessItem extends Item {

        static SuccessItem create() {
            return new AutoValue_CryptoCreateAdapter_SuccessItem(
                    R.layout.crypto_create_list_item_success);
        }
    }

    @AutoValue
    static abstract class NameItem extends Item {

        abstract String name();

        static NameItem create(String name) {
            return new AutoValue_CryptoCreateAdapter_NameItem(R.layout.crypto_create_list_item_name,
                    name);
        }
    }

    @AutoValue
    static abstract class SubheadItem extends Item {

        @StringRes abstract int title();

        static SubheadItem create(@StringRes int title) {
            return new AutoValue_CryptoCreateAdapter_SubheadItem(
                    R.layout.crypto_create_list_item_subhead, title);
        }
    }

    @AutoValue
    static abstract class AddButtonItem extends Item {

        @StringRes abstract int text();

        static AddButtonItem create(@StringRes int text) {
            return new AutoValue_CryptoCreateAdapter_AddButtonItem(
                    R.layout.crypto_create_list_item_add_button, text);
        }
    }
}
