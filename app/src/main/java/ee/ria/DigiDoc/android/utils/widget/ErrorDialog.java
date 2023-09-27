package ee.ria.DigiDoc.android.utils.widget;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.SecureUtil;

public class ErrorDialog extends AlertDialog {

    public ErrorDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(context, getWindow());
    }

    public ErrorDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
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
