package ee.ria.DigiDoc.android.signature.update;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.utils.Formatter;
import io.reactivex.Observable;

import static android.support.v4.content.res.ResourcesCompat.getColor;
import static com.jakewharton.rxbinding2.view.RxView.clicks;

final class SignatureUpdateAdapter extends
        RecyclerView.Adapter<SignatureUpdateAdapter.UpdateViewHolder<SignatureUpdateAdapter.Item>> {

    private ImmutableList<Item> items = ImmutableList.of();

    void setData(ImmutableList<Document> documents, ImmutableList<Signature> signatures) {
        ImmutableList<Item> items = ImmutableList.<Item>builder()
                .add(SubheadItem.create(R.string.signature_update_documents_title,
                        R.string.signature_update_documents_button, true))
                .addAll(DocumentItem.of(documents))
                .add(SubheadItem.create(R.string.signature_update_signatures_title,
                        R.string.signature_update_signatures_button, true))
                .addAll(SignatureItem.of(signatures))
                .build();

        DiffUtil.DiffResult result = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.items, items));
        this.items = items;
        result.dispatchUpdatesTo(this);
    }

    Observable<Document> documentClicks() {
        return Observable.empty();
    }

    Observable<Object> documentAddClicks() {
        return Observable.empty();
    }

    Observable<Document> documentRemoveClicks() {
        return Observable.empty();
    }

    Observable<Object> signatureAddClicks() {
        return Observable.empty();
    }

    Observable<Signature> signatureRemoveClicks() {
        return Observable.empty();
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
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static abstract class UpdateViewHolder<T extends Item> extends RecyclerView.ViewHolder {

        UpdateViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(T item);

        static UpdateViewHolder create(int viewType, View itemView) {
            switch (viewType) {
                case R.layout.signature_update_list_item_subhead:
                    return new SubheadViewHolder(itemView);
                case R.layout.signature_update_list_item_document:
                    return new DocumentViewHolder(itemView);
                case R.layout.signature_update_list_item_signature:
                    return new SignatureViewHolder(itemView);
                default:
                    throw new IllegalArgumentException("Unknown view type " + viewType);
            }
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
        void bind(SubheadItem item) {
            titleView.setText(item.titleRes());
            buttonView.setContentDescription(buttonView.getResources().getString(item.buttonRes()));
            buttonView.setVisibility(item.buttonVisible() ? View.VISIBLE : View.GONE);
        }
    }

    static final class DocumentViewHolder extends UpdateViewHolder<DocumentItem> {

        private final Formatter formatter;

        private final ImageView iconView;
        private final TextView nameView;
        private final TextView sizeView;
        private final ImageButton removeButton;

        DocumentViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            iconView = itemView.findViewById(R.id.signatureUpdateListDocumentIcon);
            nameView = itemView.findViewById(R.id.signatureUpdateListDocumentName);
            sizeView = itemView.findViewById(R.id.signatureUpdateListDocumentSize);
            removeButton = itemView.findViewById(R.id.signatureUpdateListDocumentRemoveButton);
        }

        @Override
        void bind(DocumentItem item) {
            clicks(itemView).subscribe();
            iconView.setImageResource(formatter.documentTypeImageRes(item.document()));
            nameView.setText(item.document().name());
            sizeView.setText(formatter.fileSize(item.document().size()));
        }
    }

    static final class SignatureViewHolder extends UpdateViewHolder<SignatureItem> {

        private final Formatter formatter;

        private final ColorStateList colorValid;
        private final ColorStateList colorInvalid;

        private final ImageView validityView;
        private final TextView nameView;
        private final TextView createdAtView;
        private final ImageButton removeButton;

        SignatureViewHolder(View itemView) {
            super(itemView);
            formatter = Application.component(itemView.getContext()).formatter();
            Resources resources = itemView.getResources();
            colorValid = ColorStateList.valueOf(getColor(resources, R.color.success, null));
            colorInvalid = ColorStateList.valueOf(getColor(resources, R.color.error, null));
            validityView = itemView.findViewById(R.id.signatureUpdateListSignatureValidity);
            nameView = itemView.findViewById(R.id.signatureUpdateListSignatureName);
            createdAtView = itemView.findViewById(R.id.signatureUpdateListSignatureCreatedAt);
            removeButton = itemView.findViewById(R.id.signatureUpdateListSignatureRemoveButton);
        }

        @Override
        void bind(SignatureItem item) {
            clicks(itemView).subscribe();
            validityView.setImageResource(item.signature().valid()
                    ? R.drawable.ic_check_circle
                    : R.drawable.ic_error);
            validityView.setImageTintList(item.signature().valid() ? colorValid : colorInvalid);
            nameView.setText(item.signature().name());
            createdAtView.setText(formatter.instant(item.signature().createdAt()));
        }
    }

    static abstract class Item {

        abstract int type();
    }

    @AutoValue
    static abstract class SubheadItem extends Item {

        @StringRes abstract int titleRes();

        @StringRes abstract int buttonRes();

        abstract boolean buttonVisible();

        static SubheadItem create(@StringRes int titleRes, @StringRes int buttonRes,
                                  boolean buttonVisible) {
            return new AutoValue_SignatureUpdateAdapter_SubheadItem(
                    R.layout.signature_update_list_item_subhead, titleRes, buttonRes,
                    buttonVisible);
        }
    }

    @AutoValue
    static abstract class DocumentItem extends Item {

        abstract Document document();

        static DocumentItem create(Document document) {
            return new AutoValue_SignatureUpdateAdapter_DocumentItem(
                    R.layout.signature_update_list_item_document, document);
        }

        static ImmutableList<DocumentItem> of(ImmutableList<Document> documents) {
            ImmutableList.Builder<DocumentItem> builder = ImmutableList.builder();
            for (Document document : documents) {
                builder.add(create(document));
            }
            return builder.build();
        }
    }

    @AutoValue
    static abstract class SignatureItem extends Item {

        abstract Signature signature();

        static SignatureItem create(Signature signature) {
            return new AutoValue_SignatureUpdateAdapter_SignatureItem(
                    R.layout.signature_update_list_item_signature, signature);
        }

        static ImmutableList<SignatureItem> of(ImmutableList<Signature> signatures) {
            ImmutableList.Builder<SignatureItem> builder = ImmutableList.builder();
            for (Signature signature : signatures) {
                builder.add(create(signature));
            }
            return builder.build();
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
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
