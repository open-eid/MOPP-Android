package ee.ria.DigiDoc.android.utils.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Constants;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class ConfirmationDialog extends AlertDialog implements
        DialogInterface.OnClickListener {

    private final Subject<Integer> buttonClicksSubject = PublishSubject.create();

    private final int action;

    public ConfirmationDialog(@NonNull Context context, @StringRes int message, int action) {
        super(context);
        SecureUtil.markAsSecure(getWindow());
        setMessage(context.getString(message));
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), this);

        this.action = action;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button okButton = getButton(BUTTON_POSITIVE);
        okButton.setContentDescription(getDialogActionConfirmationDescription(action));
        Button cancelButton = getButton(BUTTON_NEGATIVE);
        cancelButton.setContentDescription(getDialogActionCancelDescription(action));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        buttonClicksSubject.onNext(which);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String confirmationDialogDescription = getContext().getResources().getString(R.string.confirmation_dialog);
            event.getText().add(confirmationDialogDescription + ",");
            return true;
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    public Observable<Object> positiveButtonClicks() {
        return buttonClicksSubject
                .filter(which -> which == BUTTON_POSITIVE)
                .map(ignored -> Constants.VOID);
    }

    public Observable<Object> cancels() {
        return RxDialog.cancels(this)
                .mergeWith(buttonClicksSubject.filter(which -> which == BUTTON_NEGATIVE));
    }

    private String getDialogActionConfirmationDescription(int action) {
        if (action == R.id.documentRemovalDialog) {
            return getContext().getString(R.string.signature_update_confirm_file_removal_button);
        } else if (action == R.id.signatureRemovalDialog) {
            return getContext().getString(R.string.signature_update_confirm_signature_removal_button);
        }

        return null;
    }

    private String getDialogActionCancelDescription(int action) {
        if (action == R.id.documentRemovalDialog) {
            return getContext().getString(R.string.signature_update_cancel_file_removal_button);
        } else if (action == R.id.signatureRemovalDialog) {
            return getContext().getString(R.string.signature_update_cancel_signature_removal_button);
        }

        return null;
    }
}
