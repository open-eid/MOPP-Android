package ee.ria.EstEIDUtility.util;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;

public class NotificationUtil {

    private RelativeLayout success;
    private TextView successText;
    private RelativeLayout fail;
    private TextView failText;
    private RelativeLayout warning;
    private TextView warningText;

    private ImageView failClose;
    private ImageView warningClose;

    public NotificationUtil(Activity activity) {
        success = (RelativeLayout) activity.findViewById(R.id.success);
        fail = (RelativeLayout) activity.findViewById(R.id.fail);
        warning = (RelativeLayout) activity.findViewById(R.id.warning);
        failClose = (ImageView) activity.findViewById(R.id.fail_close);
        warningClose = (ImageView) activity.findViewById(R.id.warning_close);
        iconsListeners();
        messageTexts();
    }

    public NotificationUtil(View layout) {
        success = (RelativeLayout) layout.findViewById(R.id.success);
        fail = (RelativeLayout) layout.findViewById(R.id.fail);
        warning = (RelativeLayout) layout.findViewById(R.id.warning);
        failClose = (ImageView) layout.findViewById(R.id.fail_close);
        warningClose = (ImageView) layout.findViewById(R.id.warning_close);
        iconsListeners();
        messageTexts();
    }

    private void iconsListeners() {
        failClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fail.setVisibility(View.GONE);
            }
        });
        warningClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                warning.setVisibility(View.GONE);
            }
        });
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
