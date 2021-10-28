package ee.ria.DigiDoc.android.utils;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

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
}
