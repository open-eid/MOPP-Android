package ee.ria.DigiDoc.android.utils.display;

import android.content.res.Resources;
import android.util.DisplayMetrics;

public class DisplayUtil {

    public static int getDisplayMetricsDpToInt(Resources resources, int px) {
        final float scale = resources.getDisplayMetrics().density;
        return (int) (px * scale + 0.5f);
    }

    public static int getDisplayMetricsDpToInt(DisplayMetrics displayMetrics, int px) {
        return (int) (px * displayMetrics.density + 0.5f);
    }
}
