package ee.ria.DigiDoc.android.utils;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

public final class TintUtils {

    public static void tintCompoundDrawables(TextView textView) {
        ColorStateList tintList = textView.getTextColors();
        Drawable[] drawables = textView.getCompoundDrawablesRelative();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.setTintList(tintList);
            }
        }
    }

    private TintUtils() {}
}
