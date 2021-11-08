package ee.ria.DigiDoc.common;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class ActivityUtil {

    public static void restartActivity(Context context, Activity activity) {
        activity.setResult(Activity.RESULT_CANCELED, activity.getIntent());
        activity.finish();
        PackageManager packageManager = context.getPackageManager();
        android.content.Intent packageIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = packageIntent.getComponent();
        android.content.Intent restartIntent = android.content.Intent.makeRestartActivityTask(componentName);
        context.startActivity(restartIntent);
    }

    public static boolean isExternalFileOpened(Activity activity) {
        return activity != null && activity.getIntent() != null && activity.getIntent().getAction() != null &&
                (activity.getIntent().getAction().equals(Intent.ACTION_SEND) ||
                activity.getIntent().getAction().equals(Intent.ACTION_VIEW) ||
                activity.getIntent().getAction().equals(Intent.ACTION_GET_CONTENT));
    }
}