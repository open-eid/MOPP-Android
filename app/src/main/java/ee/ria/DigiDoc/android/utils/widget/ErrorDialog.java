package ee.ria.DigiDoc.android.utils.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.accessibility.AccessibilityEvent;

import ee.ria.DigiDoc.R;

public class ErrorDialog extends AlertDialog {

    public ErrorDialog(@NonNull Context context) {
        super(context);
    }

    public ErrorDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
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
