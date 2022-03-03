package ee.ria.DigiDoc.android.utils.display;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class DisplayUtil {

    private static final double PORTRAIT_WIDTH_MULTIPLIER = 0.9;
    private static final double LANDSCAPE_WIDTH_MULTIPLIER = 0.8;

    public static int getDisplayMetricsDpToInt(Resources resources, int px) {
        final float scale = resources.getDisplayMetrics().density;
        return (int) (px * scale + 0.5f);
    }

    public static int getDisplayMetricsDpToInt(DisplayMetrics displayMetrics, int px) {
        return (int) (px * displayMetrics.density + 0.5f);
    }

    public static int getDeviceOrientation(Context context) {
        return context.getResources().getConfiguration().orientation;
    }

    public static int getDeviceWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getDialogPortraitWidth() {
        return (int) (getDeviceWidth() * PORTRAIT_WIDTH_MULTIPLIER);
    }

    public static int getDialogLandscapeWidth() {
        return (int) (getDeviceWidth() * LANDSCAPE_WIDTH_MULTIPLIER);
    }

    public static int getDeviceLayoutWidth(Context context) {
        if (getDeviceOrientation(context) == Configuration.ORIENTATION_PORTRAIT) {
            return getDialogPortraitWidth();
        } else {
            return getDialogLandscapeWidth();
        }
    }
}
