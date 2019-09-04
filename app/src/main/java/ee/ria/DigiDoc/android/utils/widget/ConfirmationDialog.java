package ee.ria.DigiDoc.android.utils.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.accessibility.AccessibilityEvent;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Constants;
import ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class ConfirmationDialog extends AlertDialog implements
        DialogInterface.OnClickListener {

    private final Subject<Integer> buttonClicksSubject = PublishSubject.create();
    private String dialogMessage;

    public ConfirmationDialog(@NonNull Context context, @StringRes int message) {
        super(context);
        dialogMessage = context.getString(message);
        setMessage(dialogMessage);
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), this);

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
}
