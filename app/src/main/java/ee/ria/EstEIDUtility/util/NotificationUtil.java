package ee.ria.EstEIDUtility.util;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import ee.ria.EstEIDUtility.R;

public class NotificationUtil {

    private enum NotificationType {
        SUCCESS, ERROR, WARNING
    }

    public enum NotificationDuration {
        SHORT(2), MEDIUM(3), LONG(5);

        public int duration;

        NotificationDuration(int duration) {
            this.duration = duration;
        }
    }

    public static void showSuccess(Activity activity, int resourceId, NotificationDuration duration) {
        showNotification(activity, resourceId, duration, NotificationType.SUCCESS);
    }

    public static void showWarning(Activity activity, int resourceId, NotificationDuration duration) {
        showNotification(activity, resourceId, duration, NotificationType.WARNING);
    }

    public static void showError(FragmentActivity activity, int resourceId, NotificationDuration duration) {
        showNotification(activity, resourceId, duration, NotificationType.ERROR);
    }

    private static void showNotification(Activity activity, int resourceId, NotificationDuration duration, NotificationType toastType) {
        View layout = getLayout(activity, toastType);

        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(activity.getText(resourceId));

        createAndShowToast(activity, layout, duration);
    }

    private static void createAndShowToast(Activity activity, View layout, NotificationDuration duration) {
        Toast toast = new Toast(activity);
        toast.setGravity(Gravity.CENTER, 0, 0);
        if (duration == null) {
            toast.setDuration(NotificationDuration.MEDIUM.duration);
        } else {
            toast.setDuration(duration.duration);
        }
        toast.setView(layout);
        toast.show();
    }

    private static View getLayout(Activity activity, NotificationType toastType) {
        LayoutInflater inflater = activity.getLayoutInflater();
        switch (toastType) {
            case SUCCESS:
                return inflater.inflate(R.layout.success_toast, (ViewGroup) activity.findViewById(R.id.success_toast_container));
            case ERROR:
                return inflater.inflate(R.layout.fail_toast, (ViewGroup) activity.findViewById(R.id.fail_toast_container));
            case WARNING:
                return inflater.inflate(R.layout.warning_toast, (ViewGroup) activity.findViewById(R.id.warning_toast_container));
        }
        return null;
    }
}
