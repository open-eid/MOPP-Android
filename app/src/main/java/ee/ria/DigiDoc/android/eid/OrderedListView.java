package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import android.text.style.BulletSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

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
        String itemText = getResources().getString(itemRes);
        SpannableString spannableString = new SpannableString(itemText);
        spannableString.setSpan(new BulletSpan(20), 0, itemText.length(), 0);

        TextView textView = new TextView(getContext());
        textView.setText(spannableString);
        return textView;
    }
}
