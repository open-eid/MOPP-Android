package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.StringRes;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.Locale;

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
    final Subject<DataFile> documentClicksSubject = PublishSubject.create();
    final Subject<Object> documentAddClicksSubject = PublishSubject.create();
    final Subject<DataFile> documentRemoveClicksSubject = PublishSubject.create();
    final Subject<Signature> signatureClicksSubject = PublishSubject.create();
    final Subject<Signature> signatureRemoveClicksSubject = PublishSubject.create();

    private ImmutableList<Item> items = ImmutableList.of();

    void setData(boolean isSuccess, boolean isExistingContainer, boolean isNestedContainer,
                 @Nullable SignedContainer container) {
        boolean isWarning = container != null && !container.signaturesValid();
        String name = container == null ? null : container.name();

        ImmutableList.Builder<Item> builder = ImmutableList.builder();
        if (isSuccess) {
            builder.add(SuccessItem.create());
        }
        if (isWarning) {
            builder.add(WarningItem.create());
        }
        if (container != null) {
            builder.add(NameItem.create(name))
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
                (isWarning && !containsType(this.items, WarningItem.class)) ||
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
                case R.layout.signature_update_list_item_warning:
                    return new WarningViewHolder(itemView);
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

    static final class WarningViewHolder extends UpdateViewHolder<WarningItem> {

        WarningViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, WarningItem item) {
        }
    }

    static final class NameViewHolder extends UpdateViewHolder<NameItem> {

        private final TextView nameView;

        NameViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.signatureUpdateListName);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, NameItem item) {
            nameView.setText(item.name());
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

        private final ColorStateList textColor;
        private final ColorStateList colorValid;
        private final ColorStateList colorInvalid;

        private final TextView nameView;
        private final TextView createdAtView;
        private final ImageButton removeButton;

        SignatureViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            Resources resources = itemView.getResources();
            TypedArray a = itemView.getContext().obtainStyledAttributes(
                    new int[]{android.R.attr.textColorPrimary});
            textColor = a.getColorStateList(0);
            a.recycle();
            colorValid = ColorStateList.valueOf(getColor(resources, R.color.success, null));
            colorInvalid = ColorStateList.valueOf(getColor(resources, R.color.error, null));
            nameView = itemView.findViewById(R.id.signatureUpdateListSignatureName);
            createdAtView = itemView.findViewById(R.id.signatureUpdateListSignatureCreatedAt);
            removeButton = itemView.findViewById(R.id.signatureUpdateListSignatureRemoveButton);
        }

        @Override
        void bind(SignatureUpdateAdapter adapter, SignatureItem item) {
            Context context = itemView.getContext();
            boolean valid = item.signature().status().equals(SignatureStatus.VALID);
            Drawable validityIcon = valid
                    ? context.getDrawable(R.drawable.ic_icon_check)
                    : context.getDrawable(R.drawable.ic_icon_alert);
            if (validityIcon == null) {
                throw new IllegalStateException("Validity icon is null");
            }
            validityIcon.setTintList(valid ? colorValid : colorInvalid);
            ImageSpan validitySpan = new ImageSpan(itemView.getContext(),
                    drawableToBitmap(validityIcon), DynamicDrawableSpan.ALIGN_BASELINE);
            SpannableString nameText = new SpannableString(String.format(Locale.US, "  %s",
                    item.signature().name()));
            nameText.setSpan(validitySpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            clicks(itemView).map(ignored ->
                    ((SignatureItem) adapter.getItem(getAdapterPosition())).signature())
                    .subscribe(adapter.signatureClicksSubject);
            nameView.setText(nameText, TextView.BufferType.SPANNABLE);
            nameView.setTextColor(valid ? textColor : colorInvalid);
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
    static abstract class WarningItem extends Item {

        static WarningItem create() {
            return new AutoValue_SignatureUpdateAdapter_WarningItem(
                    R.layout.signature_update_list_item_warning);
        }
    }

    @AutoValue
    static abstract class NameItem extends Item {

        abstract String name();

        static NameItem create(String name) {
            return new AutoValue_SignatureUpdateAdapter_NameItem(
                    R.layout.signature_update_list_item_name, name);
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
