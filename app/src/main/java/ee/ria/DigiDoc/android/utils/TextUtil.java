package ee.ria.DigiDoc.android.utils;

import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

    public static void setTextViewSizeInContainer(TextView textView) {
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                1, 16, 1, TypedValue.COMPLEX_UNIT_DIP);
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                SpannableString itemText = new SpannableString((textView).getText());
                itemText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, itemText.length(), 0);
                textView.setText(itemText);
            }
        });
    }
}
