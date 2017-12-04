package ee.ria.DigiDoc.android.document.list;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

import static ee.ria.DigiDoc.android.utils.BundleUtils.getParcelableImmutableList;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putParcelableImmutableList;

public final class DocumentListScreen extends ConductorScreen {

    private static final String DOCUMENTS = "documents";

    public static DocumentListScreen create(ImmutableList<Document> documents) {
        Bundle args = new Bundle();
        putParcelableImmutableList(args, DOCUMENTS, documents);
        return new DocumentListScreen(args);
    }

    private final ImmutableList<Document> documents;

    @SuppressWarnings("WeakerAccess")
    public DocumentListScreen(Bundle args) {
        super(R.id.documentListScreen, args);
        documents = getParcelableImmutableList(args, DOCUMENTS);
        ChangeHandler changeHandler = new ChangeHandler();
        overridePushHandler(changeHandler);
        overridePopHandler(changeHandler);
    }

    @Override
    protected View createView(Context context) {
        DocumentListView view = new DocumentListView(context);
        view.setDocuments(documents);
        return view;
    }

    public static final class ChangeHandler extends AnimatorChangeHandler {

        @NonNull
        @Override
        protected Animator getAnimator(@NonNull final ViewGroup container, @Nullable View from,
                                       @Nullable final View to, boolean isPush,
                                       boolean toAddedToContainer) {
            if (from == null || to == null) {
                return new AnimatorSet();
            }

            View containerView = isPush ? from : to;
            View listView = isPush ? to : from;
            return create(container, containerView.findViewById(R.id.signatureUpdateDocuments),
                    listView,  R.id.documentListRecycler, isPush);
        }

        @Override
        protected void resetFromView(@NonNull View from) {
        }
    }

    static Animator create(final ViewGroup container, final View card, final View screen,
                           @IdRes int listId, final boolean isPush) {
        Resources resources = container.getResources();

        final View cardList = card.findViewById(listId);
        final View screenList = screen.findViewById(listId);

        final Rect containerBounds = getBounds(container);
        final Rect cardBounds = getBounds(card, containerBounds);
        final Rect cardListBounds = getBounds(cardList, containerBounds);
        final Rect screenBounds = getBounds(screen, containerBounds);
        final Rect screenListBounds = getBounds(screenList, containerBounds);

        final int cardXDelta = cardBounds.left - screenBounds.left;
        final int cardYDelta = cardBounds.top - screenBounds.top;
        final int cardWidth = cardBounds.width();
        final int cardHeight = cardBounds.height();
        final int cardWidthDelta = screenBounds.width() - cardWidth;
        final int cardHeightDelta = screenBounds.height() - cardHeight;
        final int listXDelta = cardListBounds.left - screenListBounds.left;
        final int listYDelta = cardListBounds.top - screenListBounds.top;

        float normalElevation = resources.getDimension(R.dimen.material_card_elevation_resting);
        float raisedElevation = resources.getDimension(R.dimen.material_card_elevation_raised);
        float elevationDelta = raisedElevation - normalElevation;

        final CardView cardOverlay = new CardView(container.getContext());
        cardOverlay.setX(isPush ? cardBounds.left : screenBounds.left);
        cardOverlay.setY(isPush ? cardBounds.top : screenBounds.top);
        container.addView(cardOverlay, new ViewGroup.LayoutParams(
                isPush ? cardWidth : ViewGroup.LayoutParams.MATCH_PARENT,
                isPush ? cardHeight : ViewGroup.LayoutParams.MATCH_PARENT));
        final ViewGroup.LayoutParams cardOverlayLayoutParams = cardOverlay.getLayoutParams();
        card.setVisibility(View.INVISIBLE);

        screen.setZ(raisedElevation + 1);
        screen.setClipBounds(isPush ? cardBounds : screenBounds);
        screenList.setX(isPush ? cardListBounds.left : screenListBounds.left);
        screenList.setY(isPush ? cardListBounds.top : screenListBounds.top);

        ValueAnimator animator = ValueAnimator.ofFloat(isPush ? 0f : 1f, isPush ? 1f : 0f);
        animator.setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime));
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();

            float x = screenBounds.left + (cardXDelta * (1 - value));
            float y = screenBounds.top + (cardYDelta * (1 - value));
            int width = cardWidth + (int) (cardWidthDelta * value);
            int height = cardHeight + (int) (cardHeightDelta * value);
            float elevation = normalElevation + (elevationDelta * value);

            cardOverlay.setX(x);
            cardOverlay.setY(y);
            cardOverlay.setElevation(elevation);
            cardOverlayLayoutParams.width = width;
            cardOverlayLayoutParams.height = height;
            cardOverlay.setLayoutParams(cardOverlayLayoutParams);

            int xInt = (int) x;
            int yInt = (int) y;
            screen.setClipBounds(new Rect(xInt, yInt, xInt + width, yInt + height));

            screenList.setX(screenListBounds.left + (listXDelta * (1 - value)));
            screenList.setY(screenListBounds.top + (listYDelta * (1 - value)));
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                container.removeView(cardOverlay);
                card.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        return animator;
    }

    private static Rect getBounds(View view) {
        Rect bounds = new Rect();
        view.getGlobalVisibleRect(bounds);
        return bounds;
    }

    private static Rect getBounds(View view, Rect offset) {
        Rect bounds = getBounds(view);
        bounds.offset(-offset.left, -offset.top);
        return bounds;
    }
}
