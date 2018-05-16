package ee.ria.DigiDoc.android.signature.update;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.StringRes;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignatureStatus;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.support.v4.content.res.ResourcesCompat.getColor;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.Constants.VOID;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateAdapter.SubheadItemType.DOCUMENT;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateAdapter.SubheadItemType.SIGNATURE;
import static ee.ria.DigiDoc.android.utils.Immutables.containsType;

final class SignatureUpdateAdapter extends
        RecyclerView.Adapter<SignatureUpdateAdapter.UpdateViewHolder<SignatureUpdateAdapter.Item>> {

    final Subject<Object> scrollToTopSubject = PublishSubject.create();
    final Subject<Object> nameUpdateClicksSubject = PublishSubject.create();
    final Subject<DataFile> documentClicksSubject = PublishSubject.create();
    final Subject<Object> documentAddClicksSubject = PublishSubject.create();
    final Subject<DataFile> documentRemoveClicksSubject = PublishSubject.create();
    final Subject<Signature> signatureClicksSubject = PublishSubject.create();
    final Subject<Signature> signatureRemoveClicksSubject = PublishSubject.create();

    private ImmutableList<Item> items = ImmutableList.of();

    void setData(boolean isSuccess, boolean isExistingContainer, boolean isNestedContainer,
                 @Nullable SignedContainer container) {
        boolean signaturesValid = container == null || container.signaturesValid();
        String name = container == null ? null : container.name();

        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        if (isSuccess) {
            builder.add(SuccessItem.create());
        }
        if (!signaturesValid) {
            builder.add(StatusItem.create(container.invalidSignatureCounts()));
        }
        if (container != null) {
            builder.add(NameItem.create(name, !isNestedContainer))
                    .add(SubheadItem.create(DOCUMENT,
                            isExistingContainer && !isNestedContainer
                                    && container.dataFileAddEnabled()))
                    .addAll(DocumentItem.of(container.dataFiles(),
                            !isNestedContainer && container.dataFileRemoveEnabled()));
            if (isExistingContainer) {
                builder.add(SubheadItem.create(SIGNATURE, true));
                if (container.signatures().size() == 0) {
                    builder.add(SignaturesEmptyItem.create());
                } else {
                    builder.addAll(SignatureItem.of(container.signatures(), !isNestedContainer));
                }
            } else {
                builder.add(DocumentsAddButtonItem.create());
            }
        }
        ImmutableList<Item> items = builder.build();

        boolean shouldScrollToTop = !this.items.isEmpty() &&
                ((isSuccess && !containsType(this.items, SuccessItem.class)) ||
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

    Observable<DataFile> documentRemoveClicks() {
        return documentRemoveClicksSubject;
    }

    Observable<Signature> signatureClicks() {
        return signatureClicksSubject;
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
                    return new SuccessViewHolder(itemView);
                case R.layout.signature_update_list_item_status:
                    return new StatusViewHolder(itemView);
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
        void bind(SignatureUpdateAdapter adapter, SuccessItem item) {
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
            nameView.setText(item.name());
            updateButton.setVisibility(item.updateButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(updateButton).subscribe(adapter.nameUpdateClicksSubject);
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
        private final ImageButton removeButton;

        DocumentViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureUpdateListDocumentName);
            removeButton = itemView.findViewById(R.id.signatureUpdateListDocumentRemoveButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, DocumentItem item) {
            clicks(itemView).map(ignored ->
                    ((DocumentItem) adapter.getItem(getAdapterPosition())).document())
                    .subscribe(adapter.documentClicksSubject);
            nameView.setText(item.document().name());
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton).map(ignored ->
                    ((DocumentItem) adapter.getItem(getAdapterPosition())).document())
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
                    ((SignatureItem) adapter.getItem(getAdapterPosition())).signature())
                    .subscribe(adapter.signatureClicksSubject);
            nameView.setText(item.signature().name());
            switch (item.signature().status()) {
                case SignatureStatus.INVALID:
                    statusView.setText(R.string.signature_update_signature_status_invalid);
                    break;
                case SignatureStatus.UNKNOWN:
                    statusView.setText(R.string.signature_update_signature_status_unknown);
                    break;
                default:
                    statusView.setText(R.string.signature_update_signature_status_valid);
                    break;
            }
            statusView.setTextColor(item.signature().valid() ? colorValid : colorInvalid);
            switch (item.signature().status()) {
                case SignatureStatus.WARNING:
                    statusCautionView.setVisibility(View.VISIBLE);
                    statusCautionView.setText(R.string.signature_update_signature_status_warning);
                    break;
                case SignatureStatus.NON_QSCD:
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
            removeButton.setVisibility(item.removeButtonVisible() ? View.VISIBLE : View.GONE);
            clicks(removeButton).map(ignored ->
                    ((SignatureItem) adapter.getItem(getAdapterPosition())).signature())
                    .subscribe(adapter.signatureRemoveClicksSubject);
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

    @StringDef({DOCUMENT, SIGNATURE})
    @interface SubheadItemType {
        String DOCUMENT = "DOCUMENT";
        String SIGNATURE = "SIGNATURE";
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

        abstract ImmutableMap<String, Integer> counts();

        static StatusItem create(ImmutableMap<String, Integer> counts) {
            return new AutoValue_SignatureUpdateAdapter_StatusItem(
                    R.layout.signature_update_list_item_status, counts);
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
                buttonRes = R.string.signature_update_documents_button;
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

        static SignatureItem create(Signature signature, boolean removeButtonVisible) {
            return new AutoValue_SignatureUpdateAdapter_SignatureItem(
                    R.layout.signature_update_list_item_signature, signature, removeButtonVisible);
        }

        static ImmutableList<SignatureItem> of(ImmutableList<Signature> signatures,
                                               boolean removeButtonVisible) {
            ImmutableList.Builder<SignatureItem> builder = ImmutableList.builder();
            for (Signature signature : signatures) {
                builder.add(create(signature, removeButtonVisible));
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

    static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }
        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
