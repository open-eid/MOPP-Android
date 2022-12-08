package ee.ria.DigiDoc.android.signature.update;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.DateUtil;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.common.TextUtil;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignatureStatus;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static androidx.core.content.res.ResourcesCompat.getColor;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static ee.ria.DigiDoc.android.Constants.VOID;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateAdapter.SubheadItemType.DOCUMENT;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateAdapter.SubheadItemType.SIGNATURE;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateAdapter.SubheadItemType.TIMESTAMP;
import static ee.ria.DigiDoc.android.utils.Immutables.containsType;

final class SignatureUpdateAdapter extends
        RecyclerView.Adapter<SignatureUpdateAdapter.UpdateViewHolder<SignatureUpdateAdapter.Item>> {

    final Subject<Object> scrollToTopSubject = PublishSubject.create();
    final Subject<Object> nameUpdateClicksSubject = PublishSubject.create();
    final Subject<DataFile> documentClicksSubject = PublishSubject.create();
    final Subject<Object> documentAddClicksSubject = PublishSubject.create();
    final Subject<DataFile> documentSaveClicksSubject = PublishSubject.create();
    final Subject<DataFile> documentRemoveClicksSubject = PublishSubject.create();
    final Subject<Signature> signatureClicksSubject = PublishSubject.create();
    final Subject<Signature> signatureRemoveClicksSubject = PublishSubject.create();

    private final ImmutableList<String> ASICS_TIMESTAMP_CONTAINERS = ImmutableList.of("asics", "scs");
    private final ImmutableList<String> NO_REMOVE_SIGNATURE_BUTTON_FILE_EXTENSIONS = ImmutableList.of("ddoc", "asics", "scs", "pdf");

    private ImmutableList<Item> items = ImmutableList.of();

    void setData(boolean isSuccess, boolean isExistingContainer, boolean isNestedContainer,
                 @Nullable SignedContainer container, @Nullable File nestedFile) {
        boolean signaturesValid = container == null || container.signaturesValid();
        boolean isEmptyFileInContainer = container != null && container.hasEmptyFiles();
        String name = container == null ? null : FileUtil.sanitizeString(container.name(), "");

        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        if (isSuccess) {
            builder.add(SuccessItem.create());
        }
        if (!signaturesValid) {
            builder.add(StatusItem.create(container.invalidSignatureCounts()));
        }

        if (isEmptyFileInContainer) {
            builder.add(EmptyItem.create());
        }

        if (container != null) {
            if (nestedFile == null) {
                createRegularDataFilesView(builder, name, container, isNestedContainer, isExistingContainer);
            }

            if (isExistingContainer) {
                if (nestedFile != null) {
                    try {
                        SignedContainer signedContainerNested = SignedContainer.open(nestedFile);
                        if (!container.dataFiles().isEmpty() && container.dataFiles().size() == 1 &&
                                Files.getFileExtension(nestedFile.getName()).equalsIgnoreCase("ddoc")) {
                            createAsicsDataFilesView(builder, name, container, signedContainerNested, isNestedContainer);
                            createAsicsTimestampView(builder, container);
                        }

                        if (SignedContainer.isContainer(nestedFile) && !signedContainerNested.dataFiles().isEmpty()) {
                            createAsicsSignatureView(builder, signedContainerNested);
                        }
                    } catch (Exception e) {
                        Timber.log(Log.ERROR, e, "Unable to get nested container file to show timestamp signature");
                        createRegularDataFilesView(builder, name, container, isNestedContainer, isExistingContainer);
                        createRegularSignatureView(builder, container, isNestedContainer);
                    }
                } else {
                    if (ASICS_TIMESTAMP_CONTAINERS.contains(Files.getFileExtension(container.name()).toLowerCase())) {
                        createAsicsTimestampView(builder, container);
                    } else {
                        createRegularSignatureView(builder, container, isNestedContainer);
                    }
                }
            } else {
                builder.add(DocumentsAddButtonItem.create());
            }
        }
        ImmutableList<Item> items = builder.build();

        boolean shouldScrollToTop = !this.items.isEmpty() &&
                ((isSuccess && !containsType(this.items, SuccessItem.class)) ||
                (isEmptyFileInContainer && !containsType(this.items, EmptyItem.class)) ||
                (!signaturesValid && !containsType(this.items, StatusItem.class)) ||
                (name != null && !containsType(this.items, NameItem.class)));

        DiffUtil.DiffResult result = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.items, items));
        this.items = items;
        result.dispatchUpdatesTo(this);

        if (shouldScrollToTop) {
            scrollToTopSubject.onNext(VOID);
        }
    }

    private void createRegularDataFilesView(ImmutableList.Builder<Item> builder, String name, SignedContainer container,
                                     boolean isNestedContainer, boolean isExistingContainer) {
        builder.add(NameItem.create(name, !isNestedContainer))
                .add(SubheadItem.create(DOCUMENT,
                        isExistingContainer && !isNestedContainer
                                && container.dataFileAddEnabled()))
                .addAll(DocumentItem.of(container.dataFiles(),
                        !isNestedContainer && container.dataFileRemoveEnabled()));
    }

    private void createRegularSignatureView(ImmutableList.Builder<Item> builder, SignedContainer container, boolean isNestedContainer) {
        builder.add(SubheadItem.create(SIGNATURE, true));
        if (container.signatures().isEmpty()) {
            builder.add(SignaturesEmptyItem.create());
        } else {
            boolean showRemoveSignatureButton = !isNestedContainer &&
                    !NO_REMOVE_SIGNATURE_BUTTON_FILE_EXTENSIONS.contains(
                            Files.getFileExtension(container.name()).toLowerCase());
            builder.addAll(SignatureItem.of(container.signatures(), showRemoveSignatureButton,
                    Files.getFileExtension(container.file().getName()).equalsIgnoreCase("ddoc")));
        }
    }

    private void createAsicsDataFilesView(ImmutableList.Builder<Item> builder, String name,
                                          SignedContainer container, SignedContainer signedContainerNested,
                                          boolean isNestedContainer) {
        builder.add(NameItem.create(name, false))
                .add(SubheadItem.create(DOCUMENT,
                        !isNestedContainer
                                && container.dataFileAddEnabled()))
                .addAll(DocumentItem.of(signedContainerNested.dataFiles(), false));
    }

    private void createAsicsTimestampView(ImmutableList.Builder<Item> builder, SignedContainer container) {
        builder.add(SubheadItem.create(TIMESTAMP, false));
        builder.addAll(TimestampItem.of(container.signatures()));
    }

    private void createAsicsSignatureView(ImmutableList.Builder<Item> builder, SignedContainer signedContainerNested) {
        builder.add(SubheadItem.create(SIGNATURE, true));
        builder.addAll(SignatureItem.of(signedContainerNested.signatures(), false,
                Files.getFileExtension(signedContainerNested.file().getName()).equalsIgnoreCase("ddoc")));
    }

    Observable<Object> scrollToTop() {
        return scrollToTopSubject;
    }

    Observable<Object> nameUpdateClicks() {
        return nameUpdateClicksSubject;
    }

    Observable<DataFile> documentClicks() {
        return documentClicksSubject;
    }

    Observable<Object> documentAddClicks() {
        return documentAddClicksSubject;
    }

    Observable<DataFile> documentSaveClicks() {
        return documentSaveClicksSubject;
    }

    Observable<Signature> signatureClicks() {
        return signatureClicksSubject;
    }

    Observable<DataFile> documentRemoveClicks() {
        return documentRemoveClicksSubject;
    }

    Observable<Signature> signatureRemoveClicks() {
        return signatureRemoveClicksSubject;
    }

    Item getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type();
    }

    @Override
    public UpdateViewHolder<Item> onCreateViewHolder(ViewGroup parent, int viewType) {
        //noinspection unchecked
        return UpdateViewHolder.create(viewType, LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(UpdateViewHolder<Item> holder, int position) {
        holder.bind(this, getItem(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static abstract class UpdateViewHolder<T extends Item> extends RecyclerView.ViewHolder {

        UpdateViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(SignatureUpdateAdapter adapter, T item);

        static UpdateViewHolder create(int viewType, View itemView) {
            switch (viewType) {
                case R.layout.signature_update_list_item_success:
                    if (AccessibilityUtils.isAccessibilityEnabled()) {
                        AccessibilityUtils.interrupt(itemView.getContext());
                        AccessibilityUtils.sendAccessibilityEvent(itemView.getContext(), TYPE_ANNOUNCEMENT, R.string.container_signature_added);
                    }
                    return new SuccessViewHolder(itemView);
                case R.layout.signature_update_list_item_status:
                    return new StatusViewHolder(itemView);
                case R.layout.signature_update_list_item_empty:
                    return new EmptyViewHolder(itemView);
                case R.layout.signature_update_list_item_name:
                    return new NameViewHolder(itemView);
                case R.layout.signature_update_list_item_subhead:
                    return new SubheadViewHolder(itemView);
                case R.layout.signature_update_list_item_document:
                    return new DocumentViewHolder(itemView);
                case R.layout.signature_update_list_item_signature:
                    return new SignatureViewHolder(itemView);
                case R.layout.signature_update_list_item_signatures_empty:
                    return new SignaturesEmptyViewHolder(itemView);
                case R.layout.signature_update_list_item_documents_add_button:
                    return new DocumentsAddButtonViewHolder(itemView);
                case R.layout.signature_update_list_item_timestamp:
                        return new TimestampViewHolder(itemView);
                default:
                    throw new IllegalArgumentException("Unknown view type " + viewType);
            }
        }
    }

    static final class SuccessViewHolder extends UpdateViewHolder<SuccessItem> {

        SuccessViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, SuccessItem item) {}
    }

    static final class EmptyViewHolder extends UpdateViewHolder<EmptyItem> {

        private final Resources resources;

        private final TextView emptyFileView;

        EmptyViewHolder(View itemView) {
            super(itemView);
            resources = itemView.getResources();
            emptyFileView = itemView.findViewById(R.id.signatureUpdateListStatusEmptyFile);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, EmptyItem item) {
            emptyFileView.setText(resources.getString(R.string.empty_file_message));
            emptyFileView.setContentDescription(emptyFileView.getText());
            emptyFileView.setVisibility(View.VISIBLE);
        }
    }

    static final class StatusViewHolder extends UpdateViewHolder<StatusItem> {

        private final Resources resources;

        private final TextView unknownView;
        private final TextView invalidView;

        StatusViewHolder(View itemView) {
            super(itemView);
            resources = itemView.getResources();
            unknownView = itemView.findViewById(R.id.signatureUpdateListStatusUnknown);
            invalidView = itemView.findViewById(R.id.signatureUpdateListStatusInvalid);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, StatusItem item) {
            int unknownCount = item.counts().get(SignatureStatus.UNKNOWN);
            int invalidCount = item.counts().get(SignatureStatus.INVALID);

            unknownView.setText(resources.getQuantityString(
                    R.plurals.signature_update_signatures_unknown, unknownCount, unknownCount));
            invalidView.setText(resources.getQuantityString(
                    R.plurals.signature_update_signatures_invalid, invalidCount, invalidCount));

            String containerHasDesc = unknownView.getResources().getString(R.string.container_has);
            unknownView.setContentDescription(containerHasDesc + " " + unknownView.getText());
            invalidView.setContentDescription(containerHasDesc + " " + invalidView.getText());

            unknownView.setVisibility(unknownCount == 0 ? View.GONE : View.VISIBLE);
            invalidView.setVisibility(invalidCount == 0 ? View.GONE : View.VISIBLE);
        }
    }

    static final class NameViewHolder extends UpdateViewHolder<NameItem> {

        private final TextView nameView;
        private final View updateButton;

        NameViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureUpdateListName);
            updateButton = itemView.findViewById(R.id.signatureUpdateListNameUpdateButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, NameItem item) {
            if (item.name().startsWith(".")) {
                nameView.setText("newFile" + FileUtil.sanitizeString(item.name(), ""));
            } else {
                nameView.setText(FileUtil.sanitizeString(item.name(), ""));
            }
            updateButton.setVisibility(item.updateButtonVisible() && !isContainerSigned(adapter) ? View.VISIBLE : View.GONE);
            clicks(updateButton).subscribe(adapter.nameUpdateClicksSubject);
        }

        private boolean isContainerSigned(SignatureUpdateAdapter adapter) {
            for (Item signature : adapter.items) {
                if (signature instanceof SignatureItem && ((SignatureItem) signature).signature() != null) {
                    return true;
                }
            }

            return false;
        }
    }

    static final class SubheadViewHolder extends UpdateViewHolder<SubheadItem> {

        private final TextView titleView;
        private final ImageButton buttonView;

        SubheadViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.signatureUpdateListSubheadTitle);
            buttonView = itemView.findViewById(R.id.signatureUpdateListSubheadButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, SubheadItem item) {
            titleView.setText(item.titleRes());
            if (item.subheadItemType().equals(DOCUMENT)) {
                buttonView.setContentDescription(buttonView.getResources().getString(
                        item.buttonRes()));
                buttonView.setVisibility(item.buttonVisible() ? View.VISIBLE : View.INVISIBLE);
                clicks(buttonView).subscribe(adapter.documentAddClicksSubject);
            } else {
                buttonView.setVisibility(View.INVISIBLE);
            }
        }
    }

    static final class DocumentViewHolder extends UpdateViewHolder<DocumentItem> {

        private final TextView nameView;
        private final ImageButton saveButton;
        private final ImageButton removeButton;

        DocumentViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureUpdateListDocumentName);
            saveButton = itemView.findViewById(R.id.signatureUpdateListDocumentSaveButton);
            removeButton = itemView.findViewById(R.id.signatureUpdateListDocumentRemoveButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, DocumentItem item) {
            clicks(itemView).map(ignored ->
                    ((DocumentItem) adapter.getItem(getBindingAdapterPosition())).document())
                    .subscribe(adapter.documentClicksSubject);
            nameView.setText(FileUtil.sanitizeString(item.document().name(), ""));
            String fileNameDescription = nameView.getResources().getString(R.string.file);
            nameView.setContentDescription(fileNameDescription + " " + nameView.getText());

            String saveButtonText = saveButton.getResources().getString(R.string.signature_update_document_save_button);
            saveButton.setContentDescription(saveButtonText + " " + nameView.getText());
            saveButton.setVisibility(View.VISIBLE);
            clicks(saveButton).map(ignored ->
                    ((DocumentItem) adapter.getItem(getBindingAdapterPosition())).document())
                    .subscribe(adapter.documentSaveClicksSubject);

            String removeButtonText = removeButton.getResources().getString(R.string.signature_update_document_remove_button);
            removeButton.setContentDescription(removeButtonText + " " + nameView.getText());
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton).map(ignored ->
                    ((DocumentItem) adapter.getItem(getBindingAdapterPosition())).document())
                    .subscribe(adapter.documentRemoveClicksSubject);
        }
    }

    static final class SignatureViewHolder extends UpdateViewHolder<SignatureItem> {

        private final Formatter formatter;

        private final ColorStateList colorValid;
        private final ColorStateList colorInvalid;

        private final TextView nameView;
        private final TextView statusView;
        private final TextView statusCautionView;
        private final TextView createdAtView;
        private final ImageButton removeButton;

        private final Activity activityContext = (Activity)Activity.getContext().get();

        SignatureViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            Resources resources = itemView.getResources();
            colorValid = ColorStateList.valueOf(getColor(resources, R.color.success, null));
            colorInvalid = ColorStateList.valueOf(getColor(resources, R.color.error, null));
            nameView = itemView.findViewById(R.id.signatureUpdateListSignatureName);
            statusView = itemView.findViewById(R.id.signatureUpdateListSignatureStatus);
            statusCautionView = itemView
                    .findViewById(R.id.signatureUpdateListSignatureStatusCaution);
            createdAtView = itemView.findViewById(R.id.signatureUpdateListSignatureCreatedAt);
            removeButton = itemView.findViewById(R.id.signatureUpdateListSignatureRemoveButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, SignatureItem item) {
            clicks(itemView).map(ignored ->
                    ((SignatureItem) adapter.getItem(getBindingAdapterPosition())).signature())
                    .subscribe(adapter.signatureClicksSubject);
            nameView.setText(TextUtil.splitTextAndJoin(item.signature().name(), ",", ", "));
            StringBuilder nameViewAccessibility = new StringBuilder();
            String[] nameTextSplit = nameView.getText().toString().split(", ");

            for (String nameText : nameTextSplit) {
                if (TextUtil.isOnlyDigits(nameText)) {
                    nameViewAccessibility.append(TextUtil.splitTextAndJoin(nameText, "", " "));
                } else {
                    nameViewAccessibility.append(nameText);
                }
            }
            nameView.setContentDescription(nameViewAccessibility.toString().toLowerCase());
            switch (item.signature().status()) {
                case INVALID:
                    statusView.setText(R.string.signature_update_signature_status_invalid);
                    break;
                case UNKNOWN:
                    statusView.setText(R.string.signature_update_signature_status_unknown);
                    break;
                default:
                    statusView.setText(R.string.signature_update_signature_status_valid);
                    break;
            }
            statusView.setTextColor(item.signature().valid() ? colorValid : colorInvalid);
            switch (item.signature().status()) {
                case WARNING:
                    statusCautionView.setVisibility(View.VISIBLE);
                    statusCautionView.setText(R.string.signature_update_signature_status_warning);
                    break;
                case NON_QSCD:
                    statusCautionView.setVisibility(View.VISIBLE);
                    statusCautionView.setText(R.string.signature_update_signature_status_non_qscd);
                    break;
                default:
                    statusCautionView.setVisibility(View.GONE);
                    break;
            }

            Instant dateTimeInstant = DateUtil.toEpochSecond(2018, Month.JULY, 1, 0, 0, 0);
            if (item.isDdoc() && !activityContext.getSettingsDataStore().getIsDdocParentContainerTimestamped()) {
                statusCautionView.setVisibility(View.VISIBLE);
                statusCautionView.setText(R.string.signature_update_signature_status_warning);
            } else if (item.isDdoc() && !item.removeButtonVisible() && item.signature().createdAt().isBefore(dateTimeInstant)) {
                removeCautionView();
            }

            if (item.signature().status() == SignatureStatus.INVALID && statusCautionView.getVisibility() == View.VISIBLE &&
                    statusCautionView.getText().equals(statusCautionView.getResources()
                            .getString(R.string.signature_update_signature_status_warning))) {
                removeCautionView();
            }

            createdAtView.setText(itemView.getResources().getString(
                    R.string.signature_update_signature_created_at,
                    formatter.instant(item.signature().createdAt())));

            String removeButtonText = removeButton.getResources().getString(R.string.signature_update_signature_remove_button);
            removeButton.setContentDescription(removeButtonText + " " + nameView.getText());
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton).map(ignored ->
                    ((SignatureItem) adapter.getItem(getBindingAdapterPosition())).signature())
                    .subscribe(adapter.signatureRemoveClicksSubject);
        }

        private void removeCautionView() {
            statusCautionView.setVisibility(View.GONE);
            statusCautionView.setText("");
        }
    }

    static final class SignaturesEmptyViewHolder extends UpdateViewHolder<SignaturesEmptyItem> {

        SignaturesEmptyViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, SignaturesEmptyItem item) {
        }
    }

    static final class TimestampViewHolder extends UpdateViewHolder<TimestampItem> {

        private final Formatter formatter;

        private final ColorStateList colorValid;
        private final ColorStateList colorInvalid;

        private final TextView nameView;
        private final TextView statusView;
        private final TextView statusCautionView;
        private final TextView createdAtView;

        private final Activity activityContext = (Activity)Activity.getContext().get();

        TimestampViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            Resources resources = itemView.getResources();
            colorValid = ColorStateList.valueOf(getColor(resources, R.color.success, null));
            colorInvalid = ColorStateList.valueOf(getColor(resources, R.color.error, null));
            nameView = itemView.findViewById(R.id.signatureUpdateListSignatureName);
            statusView = itemView.findViewById(R.id.signatureUpdateListSignatureStatus);
            statusCautionView = itemView
                    .findViewById(R.id.signatureUpdateListSignatureStatusCaution);
            createdAtView = itemView.findViewById(R.id.signatureUpdateListSignatureCreatedAt);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, TimestampItem item) {
            clicks(itemView).map(ignored ->
                    ((TimestampItem) adapter.getItem(getBindingAdapterPosition())).signature())
                    .subscribe(adapter.signatureClicksSubject);
            nameView.setText(item.signature().name());
            switch (item.signature().status()) {
                case INVALID:
                    statusView.setText(R.string.signature_update_timestamp_status_invalid);
                    break;
                case UNKNOWN:
                    statusView.setText(R.string.signature_update_timestamp_status_unknown);
                    break;
                default:
                    statusView.setText(R.string.signature_update_timestamp_status_valid);
                    break;
            }
            statusView.setTextColor(item.signature().valid() ? colorValid : colorInvalid);
            switch (item.signature().status()) {
                case WARNING:
                    statusCautionView.setVisibility(View.VISIBLE);
                    statusCautionView.setText(R.string.signature_update_signature_status_warning);
                    break;
                case NON_QSCD:
                    statusCautionView.setVisibility(View.VISIBLE);
                    statusCautionView.setText(R.string.signature_update_signature_status_non_qscd);
                    break;
                default:
                    statusCautionView.setVisibility(View.GONE);
                    break;
            }

            createdAtView.setText(itemView.getResources().getString(
                    R.string.signature_update_signature_created_at,
                    formatter.instant(item.signature().createdAt())));
        }
    }

    static final class DocumentsAddButtonViewHolder extends
            UpdateViewHolder<DocumentsAddButtonItem> {

        private final Button documentsAddButton;

        DocumentsAddButtonViewHolder(View itemView) {
            super(itemView);
            documentsAddButton = itemView.findViewById(R.id.signatureUpdateListDocumentsAddButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, DocumentsAddButtonItem item) {
            clicks(documentsAddButton).subscribe(adapter.documentAddClicksSubject);
        }
    }

    @StringDef({DOCUMENT, SIGNATURE, TIMESTAMP})
    @Retention(RetentionPolicy.SOURCE)
    @interface SubheadItemType {
        String DOCUMENT = "DOCUMENT";
        String SIGNATURE = "SIGNATURE";
        String TIMESTAMP = "TIMESTAMP";
    }

    static abstract class Item {

        abstract int type();
    }

    @AutoValue
    static abstract class SuccessItem extends Item {

        static SuccessItem create() {
            return new AutoValue_SignatureUpdateAdapter_SuccessItem(
                    R.layout.signature_update_list_item_success);
        }
    }

    @AutoValue
    static abstract class StatusItem extends Item {

        abstract ImmutableMap<SignatureStatus, Integer> counts();

        static StatusItem create(ImmutableMap<SignatureStatus, Integer> counts) {
            return new AutoValue_SignatureUpdateAdapter_StatusItem(
                    R.layout.signature_update_list_item_status, counts);
        }
    }

    @AutoValue
    static abstract class EmptyItem extends Item {

        static EmptyItem create() {
            return new AutoValue_SignatureUpdateAdapter_EmptyItem(
                    R.layout.signature_update_list_item_empty);
        }
    }

    @AutoValue
    static abstract class NameItem extends Item {

        abstract String name();

        abstract boolean updateButtonVisible();

        static NameItem create(String name, boolean updateButtonVisible) {
            return new AutoValue_SignatureUpdateAdapter_NameItem(
                    R.layout.signature_update_list_item_name, name, updateButtonVisible);
        }
    }

    @AutoValue
    static abstract class SubheadItem extends Item {

        @SubheadItemType abstract String subheadItemType();

        @StringRes abstract int titleRes();

        @StringRes abstract int buttonRes();

        abstract boolean buttonVisible();

        static SubheadItem create(@SubheadItemType String subheadItemType, boolean buttonVisible) {
            int titleRes;
            int buttonRes;
            if (subheadItemType.equals(DOCUMENT)) {
                titleRes = R.string.signature_update_documents_title;
                buttonRes = R.string.documents_add_button_accessibility;
            } else if (subheadItemType.equals(TIMESTAMP)) {
                titleRes = R.string.signature_update_signature_type_timestamp;
                buttonRes = 0;
            } else {
                titleRes = R.string.signature_update_signatures_title;
                buttonRes = 0;
            }
            return new AutoValue_SignatureUpdateAdapter_SubheadItem(
                    R.layout.signature_update_list_item_subhead, subheadItemType, titleRes,
                    buttonRes, buttonVisible);
        }
    }

    @AutoValue
    static abstract class DocumentItem extends Item {

        abstract DataFile document();

        abstract boolean removeButtonVisible();

        static DocumentItem create(DataFile document, boolean removeButtonVisible) {
            return new AutoValue_SignatureUpdateAdapter_DocumentItem(
                    R.layout.signature_update_list_item_document, document, removeButtonVisible);
        }

        static ImmutableList<DocumentItem> of(ImmutableList<DataFile> documents,
                                              boolean removeButtonVisible) {
            ImmutableList.Builder<DocumentItem> builder = ImmutableList.builder();
            for (DataFile document : documents) {
                builder.add(create(document, removeButtonVisible));
            }
            return builder.build();
        }
    }

    @AutoValue
    static abstract class SignatureItem extends Item {

        abstract Signature signature();

        abstract boolean removeButtonVisible();

        abstract boolean isDdoc();

        static SignatureItem create(Signature signature, boolean removeButtonVisible, boolean isDdoc) {
            return new AutoValue_SignatureUpdateAdapter_SignatureItem(
                    R.layout.signature_update_list_item_signature, signature, removeButtonVisible, isDdoc);
        }

        static ImmutableList<SignatureItem> of(ImmutableList<Signature> signatures,
                                               boolean removeButtonVisible, boolean isDdoc) {
            ImmutableList.Builder<SignatureItem> builder = ImmutableList.builder();
            for (Signature signature : signatures) {
                builder.add(create(signature, removeButtonVisible, isDdoc));
            }
            return builder.build();
        }
    }

    @AutoValue
    static abstract class SignaturesEmptyItem extends Item {

        static SignaturesEmptyItem create() {
            return new AutoValue_SignatureUpdateAdapter_SignaturesEmptyItem(
                    R.layout.signature_update_list_item_signatures_empty);
        }
    }

    @AutoValue
    static abstract class TimestampItem extends Item {

        abstract Signature signature();

        static TimestampItem create(Signature signature) {
            return new AutoValue_SignatureUpdateAdapter_TimestampItem(
                    R.layout.signature_update_list_item_timestamp, signature);
        }

        static ImmutableList<TimestampItem> of(ImmutableList<Signature> signatures) {
            ImmutableList.Builder<TimestampItem> builder = ImmutableList.builder();
            for (Signature signature : signatures) {
                builder.add(create(signature));
            }
            return builder.build();
        }
    }

    @AutoValue
    static abstract class DocumentsAddButtonItem extends Item {

        static DocumentsAddButtonItem create() {
            return new AutoValue_SignatureUpdateAdapter_DocumentsAddButtonItem(
                    R.layout.signature_update_list_item_documents_add_button);
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
            if (oldItem instanceof DocumentItem && newItem instanceof DocumentItem) {
                return ((DocumentItem) oldItem).document()
                        .equals(((DocumentItem) newItem).document());
            } else {
                return oldItem.equals(newItem);
            }
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
