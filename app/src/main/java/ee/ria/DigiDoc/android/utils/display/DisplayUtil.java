package ee.ria.DigiDoc.android.utils.display;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DisplayUtil {

    private static final double PORTRAIT_WIDTH_MULTIPLIER = 0.9;
    private static final double LANDSCAPE_WIDTH_MULTIPLIER = 0.8;

    public static int getDisplayMetricsDpToInt(Resources resources, int dp) {
        final float scale = resources.getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int getDisplayMetricsDpToInt(DisplayMetrics displayMetrics, int dp) {
        return (int) (dp * displayMetrics.density + 0.5f);
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
        return getDeviceOrientation(context) == Configuration.ORIENTATION_PORTRAIT ?
                getDialogPortraitWidth() : getDialogLandscapeWidth();
    }

    public static Dialog setCustomDialogSettings(Dialog dialog) {
        WindowManager.LayoutParams layoutAttributes = dialog.getWindow().getAttributes();
        dialog.getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_PAN);
        dialog.getWindow().setAttributes(layoutAttributes);
        dialog.getWindow().setLayout(WRAP_CONTENT, MATCH_PARENT);
        return dialog;

    }
}
