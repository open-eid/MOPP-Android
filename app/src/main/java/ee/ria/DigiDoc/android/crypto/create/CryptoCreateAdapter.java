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

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.cryptolib.DataFile;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.Constants.VOID;

final class CryptoCreateAdapter extends
        RecyclerView.Adapter<CryptoCreateAdapter.CreateViewHolder<CryptoCreateAdapter.Item>> {

    final Subject<Integer> addButtonClicksSubject = PublishSubject.create();

    private ImmutableList<Item> items = ImmutableList.of();

    void setData(ImmutableList<Certificate> recipients) {
        ImmutableList.Builder<Item> builder = ImmutableList.<Item>builder()
                .add(SubheadItem.create(R.string.crypto_create_data_files_title))
                .add(DataFileItem.create(DataFile.create("data_file.xml")))
                .add(AddButtonItem.create(R.string.crypto_create_data_files_add_button))
                .add(SubheadItem.create(R.string.crypto_create_recipients_title));
        for (Certificate recipient : recipients) {
            builder.add(RecipientItem.create(recipient));
        }
        builder.add(AddButtonItem.create(R.string.crypto_create_recipients_add_button));

        items = builder.build();
        notifyDataSetChanged();
    }

    Observable<Object> recipientsAddButtonClicks() {
        return addButtonClicksSubject
                .filter(text -> text == R.string.crypto_create_recipients_add_button)
                .map(ignored -> VOID);
    }

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

        static CreateViewHolder create(@LayoutRes int viewType, View itemView) {
            switch (viewType) {
                case R.layout.crypto_create_list_item_success:
                    return new SuccessViewHolder(itemView);
                case R.layout.crypto_create_list_item_name:
                    return new NameViewHolder(itemView);
                case R.layout.crypto_list_item_subhead:
                    return new SubheadViewHolder(itemView);
                case R.layout.crypto_create_list_item_add_button:
                    return new AddButtonViewHolder(itemView);
                case R.layout.crypto_create_list_item_data_file:
                    return new DataFileViewHolder(itemView);
                case R.layout.crypto_list_item_recipient:
                    return new RecipientViewHolder(itemView);
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
            titleView = itemView.findViewById(R.id.cryptoSubheadTitle);
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
            clicks(buttonView)
                    .map(ignored ->
                            ((AddButtonItem) adapter.items.get(getAdapterPosition())).text())
                    .subscribe(adapter.addButtonClicksSubject);
        }
    }

    static final class DataFileViewHolder extends CreateViewHolder<DataFileItem> {

        private final TextView nameView;

        DataFileViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.cryptoCreateDataFileName);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, DataFileItem item) {
            nameView.setText(item.dataFile().name());
        }
    }

    static final class RecipientViewHolder extends CreateViewHolder<RecipientItem> {

        private final Formatter formatter;

        private final TextView nameView;
        private final TextView infoView;

        RecipientViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            nameView = itemView.findViewById(R.id.cryptoRecipientName);
            infoView = itemView.findViewById(R.id.cryptoRecipientInfo);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, RecipientItem item) {
            nameView.setText(item.recipient().commonName());
            infoView.setText(itemView.getResources().getString(
                    R.string.crypto_recipient_info, formatter.eidType(item.recipient().type()),
                    item.recipient().notAfter()));
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
                    R.layout.crypto_list_item_subhead, title);
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

    @AutoValue
    static abstract class DataFileItem extends Item {

        abstract DataFile dataFile();

        static DataFileItem create(DataFile dataFile) {
            return new AutoValue_CryptoCreateAdapter_DataFileItem(
                    R.layout.crypto_create_list_item_data_file, dataFile);
        }
    }

    @AutoValue
    static abstract class RecipientItem extends Item {

        abstract Certificate recipient();

        static RecipientItem create(Certificate recipient) {
            return new AutoValue_CryptoCreateAdapter_RecipientItem(
                    R.layout.crypto_list_item_recipient, recipient);
        }
    }
}
