package ee.ria.DigiDoc.android.utils.widget;

import android.app.Dialog;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;

public class NotificationDialog extends Dialog {

    public NotificationDialog(@NonNull Activity context) {
        super(context);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        setContentView(R.layout.success_notification_dialog);
        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> {
            CheckBox checkBox = (CheckBox) findViewById(R.id.successNotificationDontShowAgain);
            if (checkBox.isChecked()) {
                context.getSettingsDataStore().setShowSuccessNotification(false);
            }
            dismiss();
        });
    }

}
