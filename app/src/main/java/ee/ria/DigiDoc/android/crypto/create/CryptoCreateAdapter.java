package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

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
    final Subject<DataFile> dataFileClicksSubject = PublishSubject.create();
    final Subject<DataFile> dataFileRemoveClicksSubject = PublishSubject.create();
    final Subject<Certificate> recipientClicksSubject = PublishSubject.create();
    final Subject<Certificate> recipientRemoveClicksSubject = PublishSubject.create();
    final Subject<Certificate> recipientAddClicksSubject = PublishSubject.create();

    private ImmutableList<Item> items = ImmutableList.of();

    void dataForContainer(@Nullable File containerFile, ImmutableList<DataFile> dataFiles,
                          ImmutableList<Certificate> recipients, boolean successMessageVisible) {
        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        if (successMessageVisible) {
            builder.add(SuccessItem.create());
        }
        if (containerFile != null) {
            builder.add(NameItem.create(containerFile.getName()));
        }
        builder.add(SubheadItem.create(R.string.crypto_create_data_files_title));
        boolean dataFileRemoveButtonVisible = dataFiles.size() > 1;
        for (DataFile dataFile : dataFiles) {
            builder.add(DataFileItem.create(dataFile, dataFileRemoveButtonVisible));
        }
        builder.add(AddButtonItem.create(R.string.crypto_create_data_files_add_button))
                .add(SubheadItem.create(R.string.crypto_create_recipients_title));
        for (Certificate recipient : recipients) {
            builder.add(RecipientItem.create(recipient, true, false, false));
        }
        if (recipients.size() == 0) {
            builder.add(EmptyTextItem.create());
        }
        builder.add(AddButtonItem.create(R.string.crypto_create_recipients_add_button));
        items(builder.build());
    }

    void dataForRecipients(ImmutableList<Certificate> searchResults,
                           ImmutableList<Certificate> recipients) {
        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        for (Certificate searchResult : searchResults) {
            builder.add(RecipientItem.create(searchResult, false, true,
                    !recipients.contains(searchResult)));
        }
        if (recipients.size() > 0) {
            builder.add(SubheadItem.create(R.string.crypto_recipients_selected_subhead));
        }
        for (Certificate recipient : recipients) {
            builder.add(RecipientItem.create(recipient, true, false, false));
        }
        items(builder.build());
    }

    private void items(ImmutableList<Item> items) {
        DiffUtil.DiffResult result = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.items, items));
        this.items = items;
        result.dispatchUpdatesTo(this);
    }

    Observable<Object> dataFilesAddButtonClicks() {
        return addButtonClicksSubject
                .filter(text -> text == R.string.crypto_create_data_files_add_button)
                .map(ignored -> VOID);
    }

    Observable<DataFile> dataFileClicks() {
        return dataFileClicksSubject;
    }

    Observable<DataFile> dataFileRemoveClicks() {
        return dataFileRemoveClicksSubject;
    }

    Observable<Object> recipientsAddButtonClicks() {
        return addButtonClicksSubject
                .filter(text -> text == R.string.crypto_create_recipients_add_button)
                .map(ignored -> VOID);
    }

    Observable<Certificate> recipientRemoveClicks() {
        return recipientRemoveClicksSubject;
    }

    Observable<Certificate> recipientAddClicks() {
        return recipientAddClicksSubject;
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
                case R.layout.crypto_create_list_item_empty_text:
                    return new EmptyTextViewHolder(itemView);
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

    static final class EmptyTextViewHolder extends CreateViewHolder<EmptyTextItem> {

        EmptyTextViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, EmptyTextItem item) {}
    }

    static final class DataFileViewHolder extends CreateViewHolder<DataFileItem> {

        private final TextView nameView;
        private final View removeButton;

        DataFileViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.cryptoCreateDataFileName);
            removeButton = itemView.findViewById(R.id.cryptoCreateDataFileRemoveButton);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, DataFileItem item) {
            clicks(itemView)
                    .map(ignored ->
                            ((DataFileItem) adapter.items.get(getAdapterPosition())).dataFile())
                    .subscribe(adapter.dataFileClicksSubject);
            nameView.setText(item.dataFile().name());
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton)
                    .map(ignored ->
                            ((DataFileItem) adapter.items.get(getAdapterPosition())).dataFile())
                    .subscribe(adapter.dataFileRemoveClicksSubject);
        }
    }

    static final class RecipientViewHolder extends CreateViewHolder<RecipientItem> {

        private final Formatter formatter;

        private final TextView nameView;
        private final TextView infoView;
        private final View removeButton;
        private final Button addButton;

        RecipientViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            nameView = itemView.findViewById(R.id.cryptoRecipientName);
            infoView = itemView.findViewById(R.id.cryptoRecipientInfo);
            removeButton = itemView.findViewById(R.id.cryptoRecipientRemoveButton);
            addButton = itemView.findViewById(R.id.cryptoRecipientAddButton);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, RecipientItem item) {
            clicks(itemView)
                    .map(ignored ->
                            ((RecipientItem) adapter.items.get(getAdapterPosition())).recipient())
                    .subscribe(adapter.recipientClicksSubject);
            nameView.setText(item.recipient().commonName());
            infoView.setText(itemView.getResources().getString(
                    R.string.crypto_recipient_info, formatter.eidType(item.recipient().type()),
                    formatter.instant(item.recipient().notAfter())));
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton)
                    .map(ignored ->
                            ((RecipientItem) adapter.items.get(getAdapterPosition())).recipient())
                    .subscribe(adapter.recipientRemoveClicksSubject);
            addButton.setVisibility(item.addButtonVisible() ? View.VISIBLE : View.GONE);
            addButton.setEnabled(item.addButtonEnabled());
            addButton.setText(item.addButtonEnabled()
                    ? R.string.crypto_recipient_add_button
                    : R.string.crypto_recipient_add_button_added);
            clicks(addButton)
                    .map(ignored ->
                            ((RecipientItem) adapter.items.get(getAdapterPosition())).recipient())
                    .subscribe(adapter.recipientAddClicksSubject);
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
    static abstract class EmptyTextItem extends Item {

        static EmptyTextItem create() {
            return new AutoValue_CryptoCreateAdapter_EmptyTextItem(
                    R.layout.crypto_create_list_item_empty_text);
        }
    }

    @AutoValue
    static abstract class DataFileItem extends Item {

        abstract DataFile dataFile();

        abstract boolean removeButtonVisible();

        static DataFileItem create(DataFile dataFile, boolean removeButtonVisible) {
            return new AutoValue_CryptoCreateAdapter_DataFileItem(
                    R.layout.crypto_create_list_item_data_file, dataFile, removeButtonVisible);
        }
    }

    @AutoValue
    static abstract class RecipientItem extends Item {

        abstract Certificate recipient();

        abstract boolean removeButtonVisible();

        abstract boolean addButtonVisible();

        abstract boolean addButtonEnabled();

        static RecipientItem create(Certificate recipient, boolean removeButtonVisible,
                                    boolean addButtonVisible, boolean addButtonEnabled) {
            return new AutoValue_CryptoCreateAdapter_RecipientItem(
                    R.layout.crypto_list_item_recipient, recipient, removeButtonVisible,
                    addButtonVisible, addButtonEnabled);
        }
    }

    static final class DiffUtilCallback extends DiffUtil.Callback {

        private final ImmutableList<Item> oldList;
        private final ImmutableList<Item> newList;

        DiffUtilCallback(ImmutableList<Item> oldList, ImmutableList<Item> newList) {
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
            Item oldItem = oldList.get(oldItemPosition);
            Item newItem = newList.get(newItemPosition);
            if (oldItem instanceof DataFileItem && newItem instanceof DataFileItem) {
                return ((DataFileItem) oldItem).dataFile()
                        .equals(((DataFileItem) newItem).dataFile());
            } else if (oldItem instanceof RecipientItem && newItem instanceof RecipientItem) {
                RecipientItem oldRecipientItem = (RecipientItem) oldItem;
                RecipientItem newRecipientItem = (RecipientItem) newItem;
                return oldRecipientItem.recipient().equals(newRecipientItem.recipient()) &&
                        oldRecipientItem.removeButtonVisible()
                                == newRecipientItem.removeButtonVisible();
            }
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
