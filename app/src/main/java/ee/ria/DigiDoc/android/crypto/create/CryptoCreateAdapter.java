package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;

public final class CryptoCreateAdapter extends
        RecyclerView.Adapter<CryptoCreateAdapter.CreateViewHolder<CryptoCreateAdapter.Item>> {

    private ImmutableList<Item> items = ImmutableList
            .of(SuccessItem.create(), NameItem.create("some_cool_name.cdoc"));

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
            nameView = itemView.findViewById(R.id.cryptoCreateListName);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, NameItem item) {
            nameView.setText(item.name());
        }
    }

    static abstract class Item {

        abstract int type();
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

        public static NameItem create(String name) {
            return new AutoValue_CryptoCreateAdapter_NameItem(R.layout.crypto_create_list_item_name,
                    name);
        }
    }
}
