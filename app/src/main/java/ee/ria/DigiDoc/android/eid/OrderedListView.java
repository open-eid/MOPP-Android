package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;

public final class OrderedListView extends LinearLayout {

    public OrderedListView(Context context) {
        this(context, null);
    }

    public OrderedListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OrderedListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public OrderedListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                           int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
    }

    public void itemsRes(ImmutableList<Integer> itemsRes) {
        removeAllViews();
        for (int itemRes : itemsRes) {
            addView(createItemView(itemRes));
        }
    }

    private View createItemView(@StringRes int itemRes) {
        TextView iconView = new TextView(getContext());
        iconView.setGravity(Gravity.CENTER_VERTICAL);
        iconView.setText("\u2022");

        TextView textView = new TextView(getContext());
        textView.setText(itemRes);

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(HORIZONTAL);
        container.addView(iconView, new LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.material_baseline_grid_1x),
                LayoutParams.WRAP_CONTENT));
        container.addView(textView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));

        return container;
    }
}
