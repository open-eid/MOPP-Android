package ee.ria.DigiDoc.android.utils.navigator;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ee.ria.DigiDoc.android.utils.TextUtil.getInvisibleElementTextView;
import static ee.ria.DigiDoc.android.utils.ViewUtil.moveView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class InvisibleView implements ContentView {

    private static boolean hasInvisibleElementMoved = false;
    private static RecyclerView.OnScrollListener recyclerViewOnScrollListener;

    protected static void addInvisibleElementToObject(Context context, View view) {
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).addView(getInvisibleElementTextView(context));
        } else if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).addView(getInvisibleElementTextView(context));
        }
    }

    protected static void addInvisibleElement(Context context, View mainLayout) {
        if (mainLayout instanceof ViewGroup) {
            ((ViewGroup) mainLayout).addView(getInvisibleElementTextView(context));
        } else if (mainLayout != null && mainLayout.getParent() instanceof ViewGroup) {
            ((ViewGroup) mainLayout.getParent()).addView(getInvisibleElementTextView(context));
        }
    }

    protected static void addInvisibleElementScrollListener(RecyclerView recyclerView, View lastElementView) {
        if (!hasInvisibleElementMoved) {
            moveView(lastElementView);
            hasInvisibleElementMoved = true;
        }

        recyclerView.addOnScrollListener(
                recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount;
                if (layoutManager != null) {
                    totalItemCount = layoutManager.getItemCount();

                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                    if (lastVisibleItemPosition == totalItemCount - 1) {
                        lastElementView.setVisibility(VISIBLE);
                    } else {
                        lastElementView.setVisibility(GONE);
                    }
                }
            }
        });
    }

    public static void removeInvisibleElementScrollListener(RecyclerView recyclerView) {
        if (recyclerViewOnScrollListener != null) {
            recyclerView.removeOnScrollListener(recyclerViewOnScrollListener);
        }
        hasInvisibleElementMoved = false;
    }
}
