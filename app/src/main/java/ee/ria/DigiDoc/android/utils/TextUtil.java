package ee.ria.DigiDoc.android.utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

public class TextUtil {

    public static AppCompatEditText getTextView(View view) {
        if (view instanceof ScrollView) {
            final ScrollView scrollView = (ScrollView) view;
            final int scrollViewChildCount = scrollView.getChildCount();
            for (int i = 0; i < scrollViewChildCount; ++i) {
                final View scrollViewChild = scrollView.getChildAt(i);
                if (scrollViewChild instanceof LinearLayout) {
                    final LinearLayout linearLayout = (LinearLayout) scrollViewChild;
                    final int linearLayoutChildCount = linearLayout.getChildCount();
                    for (int j = 0; j < linearLayoutChildCount; ++j) {
                        final View linearLayoutChild = linearLayout.getChildAt(j);
                        if (linearLayoutChild instanceof AppCompatEditText) {
                            return (AppCompatEditText) linearLayoutChild;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static void handleDetailText(@Nullable String text, View detailView) {
        if (text != null && !text.isEmpty() && detailView instanceof TextView && detailView.getParent() != null) {
            ((TextView) detailView).setText(text);
            ((View) detailView.getParent()).setVisibility(VISIBLE);
        } else {
            if (detailView != null && detailView.getParent() != null) {
                ((View) detailView.getParent()).setVisibility(GONE);
            }
        }
    }
}
