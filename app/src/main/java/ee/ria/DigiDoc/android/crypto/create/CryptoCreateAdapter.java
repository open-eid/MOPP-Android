package ee.ria.DigiDoc.android.crypto.create;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.crypto.NoInternetConnectionException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static ee.ria.DigiDoc.android.Constants.VOID;

final class CryptoCreateAdapter extends
        RecyclerView.Adapter<CryptoCreateAdapter.CreateViewHolder<CryptoCreateAdapter.Item>> {

    final Subject<Object> nameUpdateClicksSubject = PublishSubject.create();
    final Subject<Integer> addButtonClicksSubject = PublishSubject.create();
    final Subject<File> dataFileClicksSubject = PublishSubject.create();
    final Subject<File> dataFileRemoveClicksSubject = PublishSubject.create();
    final Subject<File> dataFileSaveClicksSubject = PublishSubject.create();
    final Subject<Certificate> recipientClicksSubject = PublishSubject.create();
    final Subject<Certificate> recipientRemoveClicksSubject = PublishSubject.create();
    final Subject<Certificate> recipientAddClicksSubject = PublishSubject.create();

    private boolean dataFilesViewEnabled = false;

    private ImmutableList<Item> items = ImmutableList.of();

    void dataForContainer(@Nullable String name, @Nullable File containerFile, ImmutableList<File> dataFiles,
                          boolean dataFilesViewEnabled, boolean dataFilesAddEnabled,
                          boolean dataFilesRemoveEnabled, ImmutableList<Certificate> recipients,
                          boolean recipientsAddEnabled, boolean recipientsRemoveEnabled,
                          boolean encryptSuccessMessageVisible,
                          boolean decryptSuccessMessageVisible) {
        this.dataFilesViewEnabled = dataFilesViewEnabled;

        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        if (encryptSuccessMessageVisible) {
            builder.add(SuccessItem.create(R.string.crypto_create_encrypt_success_message));
        } else if (decryptSuccessMessageVisible) {
            builder.add(SuccessItem.create(R.string.crypto_create_decrypt_success_message));
        }
        if (name != null) {
            builder.add(NameItem.create(name, containerFile == null));
        }
        builder.add(SubheadItem.create(R.string.crypto_create_data_files_title));
        for (File dataFile : dataFiles) {
            builder.add(DataFileItem.create(dataFile, dataFilesRemoveEnabled, dataFilesViewEnabled));
        }
        if (dataFilesAddEnabled) {
            builder.add(AddButtonItem.create(R.string.crypto_create_data_files_add_button, R.string.crypto_create_data_files_add_button_description));
        }
        builder.add(SubheadItem.create(R.string.crypto_create_recipients_title));
        for (Certificate recipient : recipients) {
            builder.add(RecipientItem.create(recipient, recipientsRemoveEnabled, false, false));
        }
        if (recipients.size() == 0) {
            builder.add(EmptyTextItem.create(R.string.crypto_create_recipients_empty));
        }
        if (recipientsAddEnabled) {
            builder.add(AddButtonItem.create(R.string.crypto_create_recipients_add_button, R.string.crypto_create_recipients_add_button_description));
        }
        items(builder.build());
    }

    void dataForRecipients(@State String searchState,
                           @Nullable ImmutableList<Certificate> searchResults,
                           Throwable searchError,
                           ImmutableList<Certificate> recipients) {
        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        if (searchResults != null && searchResults.size() > 0) {
            for (Certificate searchResult : searchResults) {
                builder.add(RecipientItem.create(searchResult, false, true,
                        !recipients.contains(searchResult)));
            }
        } else if (searchError instanceof NoInternetConnectionException) {
            builder.add(EmptyTextItem.create(R.string.no_internet_connection));
        } else if (searchResults != null && !searchState.equals(State.ACTIVE)) {
            builder.add(EmptyTextItem.create(R.string.crypto_recipients_search_result_empty));
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

    Observable<Object> nameUpdateClicks() {
        return nameUpdateClicksSubject;
    }

    Observable<Object> dataFilesAddButtonClicks() {
        return addButtonClicksSubject
                .filter(text -> text == R.string.crypto_create_data_files_add_button)
                .map(ignored -> VOID);
    }

    Observable<File> dataFileClicks() {
        return dataFileClicksSubject
                .filter(ignored -> dataFilesViewEnabled);
    }

    Observable<File> dataFileRemoveClicks() {
        return dataFileRemoveClicksSubject;
    }

    Observable<File> dataFileSaveClicks() {
        return dataFileSaveClicksSubject
                .filter(ignored -> dataFilesViewEnabled);
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

        private final TextView messageView;

        SuccessViewHolder(View itemView) {
            super(itemView);
            messageView = itemView.findViewById(R.id.cryptoCreateSuccessMessage);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, SuccessItem item) {
            messageView.setText(item.message());
        }
    }

    static final class NameViewHolder extends CreateViewHolder<NameItem> {

        private final TextView nameView;
        private final View updateButton;

        NameViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.cryptoCreateName);
            updateButton = itemView.findViewById(R.id.cryptoCreateNameUpdateButton);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, NameItem item) {
            nameView.setText(FileUtil.sanitizeString(item.name(), '_'));
            updateButton.setVisibility(item.updateButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(updateButton).subscribe(adapter.nameUpdateClicksSubject);
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
            buttonView.setContentDescription(buttonView.getResources().getString(item.contentDescription()));
            clicks(buttonView)
                    .map(ignored ->
                            ((AddButtonItem) adapter.items.get(getBindingAdapterPosition())).text())
                    .subscribe(adapter.addButtonClicksSubject);
        }
    }

    static final class EmptyTextViewHolder extends CreateViewHolder<EmptyTextItem> {

        private final TextView textView;

        EmptyTextViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.cryptoEmptyText);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, EmptyTextItem item) {
            textView.setText(item.text());
        }
    }

    static final class DataFileViewHolder extends CreateViewHolder<DataFileItem> {

        private final TextView nameView;
        private final View saveButton;
        private final View removeButton;

        DataFileViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.cryptoCreateDataFileName);
            removeButton = itemView.findViewById(R.id.cryptoCreateDataFileRemoveButton);
            saveButton = itemView.findViewById(R.id.cryptoCreateDataFileSaveButton);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            displayMetrics.setToDefaults();

            removeButton.setMinimumHeight(DisplayUtil.getDisplayMetricsDpToInt(displayMetrics, 48));
        }

        @Override
        void bind(CryptoCreateAdapter adapter, DataFileItem item) {
            clicks(itemView)
                    .map(ignored ->
                            ((DataFileItem) adapter.items.get(getBindingAdapterPosition())).dataFile())
                    .subscribe(adapter.dataFileClicksSubject);
            nameView.setText(FileUtil.sanitizeString(item.dataFile().getName(), '_'));
            String fileNameDescription = nameView.getResources().getString(R.string.file);
            nameView.setContentDescription(fileNameDescription + " " + nameView.getText());

            String removeButtonText = removeButton.getResources().getString(R.string.crypto_create_data_file_remove_button);
            removeButton.setContentDescription(removeButtonText + " " + nameView.getText());
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton)
                    .map(ignored ->
                            ((DataFileItem) adapter.items.get(getBindingAdapterPosition())).dataFile())
                    .subscribe(adapter.dataFileRemoveClicksSubject);

            String saveButtonText = saveButton.getResources().getString(R.string.crypto_create_data_file_save_button);
            saveButton.setContentDescription(saveButtonText + " " + nameView.getText());
            saveButton.setVisibility(item.saveButtonVisible() ? View.VISIBLE: View.GONE);
            clicks(saveButton)
                    .map(ignored ->
                            ((DataFileItem) adapter.items.get(getBindingAdapterPosition())).dataFile())
                    .subscribe(adapter.dataFileSaveClicksSubject);
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
            AccessibilityUtils.disableDoubleTapToActivateFeedback(itemView.findViewById(R.id.cryptoRecipient));
            nameView = itemView.findViewById(R.id.cryptoRecipientName);
            infoView = itemView.findViewById(R.id.cryptoRecipientInfo);
            removeButton = itemView.findViewById(R.id.cryptoRecipientRemoveButton);
            addButton = itemView.findViewById(R.id.cryptoRecipientAddButton);
        }

        @Override
        void bind(CryptoCreateAdapter adapter, RecipientItem item) {
            clicks(itemView)
                    .map(ignored ->
                            ((RecipientItem) adapter.items.get(getBindingAdapterPosition())).recipient())
                    .subscribe(adapter.recipientClicksSubject);
            nameView.setText(item.recipient().commonName());
            infoView.setText(itemView.getResources().getString(
                    R.string.crypto_recipient_info, formatter.eidType(item.recipient().type()),
                    formatter.instant(item.recipient().notAfter())));

            String removeRecipientDescription = removeButton.getResources().getString(R.string.crypto_recipient_remove_button);
            removeButton.setContentDescription(removeRecipientDescription + " " + nameView.getText());
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton)
                    .map(ignored ->
                            ((RecipientItem) adapter.items.get(getBindingAdapterPosition())).recipient())
                    .subscribe(adapter.recipientRemoveClicksSubject);
            addButton.setVisibility(item.addButtonVisible() ? View.VISIBLE : View.GONE);
            addButton.setEnabled(item.addButtonEnabled());
            addButton.setText(item.addButtonEnabled()
                    ? R.string.crypto_recipient_add_button
                    : R.string.crypto_recipient_add_button_added);
            if (item.addButtonEnabled()) {
                String addRecipientDescription = addButton.getResources().getString(R.string.add_recipient);
                addButton.setContentDescription(addRecipientDescription + " " + nameView.getText());
            }
            clicks(addButton)
                    .map(ignored ->
                            ((RecipientItem) adapter.items.get(getBindingAdapterPosition())).recipient())
                    .subscribe(adapter.recipientAddClicksSubject);
        }
    }

    static abstract class Item {

        @LayoutRes abstract int type();
    }

    @AutoValue
    static abstract class SuccessItem extends Item {

        @StringRes abstract int message();

        static SuccessItem create(@StringRes int message) {
            return new AutoValue_CryptoCreateAdapter_SuccessItem(
                    R.layout.crypto_create_list_item_success, message);
        }
    }

    @AutoValue
    static abstract class NameItem extends Item {

        abstract String name();

        abstract boolean updateButtonVisible();

        static NameItem create(String name, boolean updateButtonVisible) {
            return new AutoValue_CryptoCreateAdapter_NameItem(
                    R.layout.crypto_create_list_item_name, name, updateButtonVisible
            );
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

        @StringRes abstract int contentDescription();

        static AddButtonItem create(@StringRes int text, @StringRes int contentDescription) {
            return new AutoValue_CryptoCreateAdapter_AddButtonItem(
                    R.layout.crypto_create_list_item_add_button, text, contentDescription);
        }
    }

    @AutoValue
    static abstract class EmptyTextItem extends Item {

        @StringRes abstract int text();

        static EmptyTextItem create(@StringRes int text) {
            return new AutoValue_CryptoCreateAdapter_EmptyTextItem(
                    R.layout.crypto_create_list_item_empty_text, text);
        }
    }

    @AutoValue
    static abstract class DataFileItem extends Item {

        abstract File dataFile();

        abstract boolean removeButtonVisible();

        abstract boolean saveButtonVisible();

        static DataFileItem create(File dataFile, boolean removeButtonVisible, boolean saveButtonVisible) {
            return new AutoValue_CryptoCreateAdapter_DataFileItem(
                    R.layout.crypto_create_list_item_data_file, dataFile, removeButtonVisible, saveButtonVisible);
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
