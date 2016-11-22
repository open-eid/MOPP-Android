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

    public static void showNotification(Activity activity, String message, NotificationType toastType) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = null;
        switch (toastType) {
            case SUCCESS:
                layout = inflater.inflate(R.layout.success_toast, (ViewGroup) activity.findViewById(R.id.success_toast_container));
                break;
            case ERROR:
                layout = inflater.inflate(R.layout.fail_toast, (ViewGroup) activity.findViewById(R.id.fail_toast_container));
                break;
            case WARNING:
                layout = inflater.inflate(R.layout.warning_toast, (ViewGroup) activity.findViewById(R.id.warning_toast_container));
                break;
        }

        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        Toast toast = new Toast(activity);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

}
