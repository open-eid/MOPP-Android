package ee.ria.DigiDoc.android.utils;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.widget.AppCompatEditText;

public class TextUtil {

    public static AppCompatEditText getTextView(View view) {
        if (view instanceof ScrollView) {
            ScrollView scrollView = ((ScrollView) view);
            for (int i = 0; i < scrollView.getChildCount(); i++) {
                if (scrollView.getChildAt(i) instanceof LinearLayout) {
                    LinearLayout linearLayout = ((LinearLayout) scrollView.getChildAt(i));
                    for (int j = 0; j < linearLayout.getChildCount(); j++) {
                        if (linearLayout.getChildAt(j) instanceof AppCompatEditText) {
                            return ((AppCompatEditText) linearLayout.getChildAt(j));
                        }
                    }
                }
            }
        }

        return null;
    }
}
