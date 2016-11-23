package ee.ria.EstEIDUtility.util;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import ee.ria.EstEIDUtility.R;

public class NotificationUtil {

    public enum NotificationType {
        SUCCESS, ERROR, WARNING
    }

    public enum NotificationDuration {
        SHORT(1), MEDIUM(3), LONG(5);

        int duration;

        NotificationDuration(int duration) {
            this.duration = duration;
        }
    }

    public static void showNotification(Activity activity, int resourceId, NotificationType toastType) {
        View layout = getLayout(activity, toastType);

        String message = activity.getResources().getString(resourceId);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        createAndShowToast(activity, layout);
    }

    public static void showNotification(Activity activity, String message, NotificationType toastType) {
        View layout = getLayout(activity, toastType);

        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        createAndShowToast(activity, layout);
    }

    private static void createAndShowToast(Activity activity, View layout) {
        Toast toast = new Toast(activity);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(NotificationDuration.MEDIUM.duration);
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
