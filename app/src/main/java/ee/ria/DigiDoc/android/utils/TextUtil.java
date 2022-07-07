package ee.ria.DigiDoc.android.utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.textfield.TextInputLayout;

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

    public static AppCompatTextView getTextInputLayoutAppCompatTextView(TextInputLayout textInputLayout) {
        for (int i = 0; i < textInputLayout.getChildCount(); i++) {
            final View textInputLayoutChild = textInputLayout.getChildAt(i);
            if (textInputLayoutChild instanceof LinearLayout) {
                final LinearLayout linearLayout = (LinearLayout) textInputLayoutChild;
                for (int j = 0; j < linearLayout.getChildCount(); j++) {
                    final View linearLayoutChild = linearLayout.getChildAt(j);
                    if (linearLayoutChild instanceof FrameLayout) {
                        final FrameLayout frameLayout = (FrameLayout) linearLayoutChild;
                        for (int k = 0; k < frameLayout.getChildCount(); k++) {
                            final View frameLayoutChild = frameLayout.getChildAt(k);
                            if (frameLayoutChild instanceof AppCompatTextView) {
                                return (AppCompatTextView) frameLayoutChild;
                            }
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
