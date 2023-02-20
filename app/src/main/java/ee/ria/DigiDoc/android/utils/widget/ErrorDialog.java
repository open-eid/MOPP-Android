package ee.ria.DigiDoc.android.utils.widget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.accessibility.AccessibilityEvent;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.SecureUtil;

public class ErrorDialog extends AlertDialog {

    public ErrorDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(context, getWindow());
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.getText().add(this.getContext().getResources().getString(R.string.error_dialog));
            return true;
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }
}
