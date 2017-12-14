package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.concurrent.atomic.AtomicInteger;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.utils.Formatter;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.support.v4.content.res.ResourcesCompat.getColor;
import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static com.jakewharton.rxbinding2.view.RxView.clicks;

final class SignatureAdapter extends RecyclerView.Adapter<SignatureAdapter.SignatureViewHolder> {

    private final Subject<Optional<Signature>> removeSelectionsSubject = PublishSubject.create();
    private final Subject<Signature> removesSubject = PublishSubject.create();

    private final TouchCallback touchCallback = new TouchCallback();

    private final ColorStateList colorValid;
    private final ColorStateList colorInvalid;
    private final Formatter formatter;

    private ImmutableList<Signature> signatures = ImmutableList.of();
    @Nullable private Signature removeSelection;

    SignatureAdapter(Context context) {
        //noinspection Guava
        touchCallback.removingItemPosition()
                .map(removing -> removing == NO_POSITION
                        ? Optional.<Signature>absent()
                        : Optional.of(signatures.get(removing)))
                .subscribe(removeSelectionsSubject);

        Resources resources = context.getResources();
        colorValid = ColorStateList.valueOf(getColor(resources, R.color.success, null));
        colorInvalid = ColorStateList.valueOf(getColor(resources, R.color.error, null));
        formatter = Application.component(context).formatter();
    }

    void setSignatures(ImmutableList<Signature> signatures) {
        DiffUtil.DiffResult diffResult = DiffUtil
                .calculateDiff(new DiffUtilCallback(this.signatures, signatures));
        this.signatures = signatures;
        diffResult.dispatchUpdatesTo(this);
    }

    Observable<Optional<Signature>> removeSelections() {
        return removeSelectionsSubject;
    }

    void setRemoveSelection(@Nullable Signature removeSelection) {
        if (this.removeSelection != null) {
            notifyItemChanged(signatures.indexOf(this.removeSelection));
        }
        this.removeSelection = removeSelection;
        if (this.removeSelection != null) {
            int position = signatures.indexOf(this.removeSelection);
            notifyItemChanged(position);
            touchCallback.setRemoving(position);
        } else {
            touchCallback.setRemoving(NO_POSITION);
        }
    }

    Observable<Signature> removes() {
        return removesSubject;
    }

    @Override
    public SignatureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SignatureViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.signature_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(SignatureViewHolder holder, int position) {
        Resources resources = holder.itemView.getResources();

        Signature signature = signatures.get(position);

        holder.nameView.setText(signature.name());
        holder.createdAtView.setText(formatter.instant(signature.createdAt()));
        if (signature.valid()) {
            holder.validityView.setImageResource(R.drawable.ic_check_circle);
            holder.validityView.setContentDescription(resources.getString(
                    R.string.signature_update_signature_valid));
            holder.validityView.setImageTintList(colorValid);
        } else {
            holder.validityView.setImageResource(R.drawable.ic_error);
            holder.validityView.setContentDescription(resources.getString(
                    R.string.signature_update_signature_invalid));
            holder.validityView.setImageTintList(colorInvalid);
        }

        holder.container.setVisibility(Objects.equal(signature, removeSelection)
                ? View.INVISIBLE
                : View.VISIBLE);
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(holder.container);

        clicks(holder.container).subscribe();
        clicks(holder.removeContainer)
                .map(ignored -> Optional.<Signature>absent())
                .subscribe(removeSelectionsSubject);
        clicks(holder.removeButton)
                .map(ignored -> signatures.get(holder.getAdapterPosition()))
                .subscribe(removesSubject);
    }

    @Override
    public int getItemCount() {
        return signatures.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        new ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView);
    }

    static final class SignatureViewHolder extends RecyclerView.ViewHolder {

        final View removeContainer;
        final ImageButton removeButton;
        final View container;
        final TextView nameView;
        final TextView createdAtView;
        final ImageView validityView;

        SignatureViewHolder(View itemView) {
            super(itemView);
            removeContainer = itemView.findViewById(R.id.signatureUpdateSignatureRemoveContainer);
            removeButton = itemView.findViewById(R.id.signatureUpdateSignatureRemoveButton);
            container = itemView.findViewById(R.id.signatureUpdateSignatureContainer);
            nameView = itemView.findViewById(R.id.signatureUpdateSignatureName);
            createdAtView = itemView.findViewById(R.id.signatureUpdateSignatureCreatedAt);
            validityView = itemView.findViewById(R.id.signatureUpdateSignatureValidity);
        }
    }

    static final class DiffUtilCallback extends DiffUtil.Callback {

        private final ImmutableList<Signature> oldList;
        private final ImmutableList<Signature> newList;

        DiffUtilCallback(ImmutableList<Signature> oldList, ImmutableList<Signature> newList) {
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
            return oldList.get(oldItemPosition).id().equals(newList.get(newItemPosition).id());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    static final class TouchCallback extends ItemTouchHelper.SimpleCallback {

        private final Subject<Integer> removingSubject = PublishSubject.create();
        private final AtomicInteger removing = new AtomicInteger(NO_POSITION);

        TouchCallback() {
            super(0, ItemTouchHelper.START);
            removingSubject.subscribe(this::setRemoving);
        }

        Observable<Integer> removingItemPosition() {
            return removingSubject;
        }

        void setRemoving(int removing) {
            this.removing.set(removing);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            if (removing.get() == NO_POSITION) {
                getDefaultUIUtil().onDraw(c, recyclerView, getSwipeView(viewHolder), dX, dY,
                        actionState, isCurrentlyActive);
            }
        }

        @Override
        public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
            if (removing.get() == NO_POSITION) {
                getDefaultUIUtil().onDrawOver(c, recyclerView, getSwipeView(viewHolder), dX, dY,
                        actionState, isCurrentlyActive);
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (removing.get() == NO_POSITION) {
                getDefaultUIUtil().clearView(getSwipeView(viewHolder));
            }
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder,
                                      int actionState) {
            if (viewHolder != null && removing.get() == NO_POSITION) {
                getDefaultUIUtil().onSelected(getSwipeView(viewHolder));
            }
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if (removing.get() == NO_POSITION) {
                removingSubject.onNext(viewHolder.getAdapterPosition());
            }
        }

        private static View getSwipeView(RecyclerView.ViewHolder viewHolder) {
            return ((SignatureViewHolder) viewHolder).container;
        }
    }
}
