package ee.ria.DigiDoc.android.utils;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

public final class TintUtils {

    public static void tintCompoundDrawables(TextView textView) {
        tintCompoundDrawables(textView, false);
    }

    public static void tintCompoundDrawables(TextView textView, boolean mutate) {
        ColorStateList tintList = textView.getTextColors();
        Drawable[] drawables = textView.getCompoundDrawablesRelative();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                if (mutate) {
                    drawable = drawable.mutate();
                }
                drawable.setTintList(tintList);
            }
        }
    }

    private TintUtils() {}
}
