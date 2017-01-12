package ee.ria.EstEIDUtility.util;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;

public class NotificationUtil {

    private LinearLayout success;
    private TextView successText;
    private LinearLayout fail;
    private TextView failText;
    private LinearLayout warning;
    private TextView warningText;

    public NotificationUtil(Activity activity) {
        success = (LinearLayout) activity.findViewById(R.id.success);
        fail = (LinearLayout) activity.findViewById(R.id.fail);
        warning = (LinearLayout) activity.findViewById(R.id.warning);
        messageTexts();
    }

    public NotificationUtil(View layout) {
        success = (LinearLayout) layout.findViewById(R.id.success);
        fail = (LinearLayout) layout.findViewById(R.id.fail);
        warning = (LinearLayout) layout.findViewById(R.id.warning);
        messageTexts();
    }

    private void messageTexts() {
        successText = (TextView) success.findViewById(R.id.text);
        failText = (TextView) fail.findViewById(R.id.text);
        warningText = (TextView) warning.findViewById(R.id.text);
    }

    public void showSuccessMessage(CharSequence message) {
        fail.setVisibility(View.GONE);
        warning.setVisibility(View.GONE);
        successText.setText(message);
        success.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                success.setVisibility(View.GONE);
            }
        }, 5000);
    }

    public void showFailMessage(CharSequence message) {
        failText.setText(message);
        fail.setVisibility(View.VISIBLE);
    }

    public void showWarningMessage(CharSequence message) {
        warningText.setText(message);
        warning.setVisibility(View.VISIBLE);
    }

    public void clearMessages() {
        fail.setVisibility(View.GONE);
        warning.setVisibility(View.GONE);
        success.setVisibility(View.GONE);
    }

}
