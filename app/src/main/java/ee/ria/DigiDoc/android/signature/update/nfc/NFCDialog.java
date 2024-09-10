package ee.ria.DigiDoc.android.signature.update.nfc;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class NFCDialog extends AlertDialog implements DialogInterface.OnClickListener {

    private TextView messageView;
    private ImageView iconView;

    private final Subject<Integer> buttonClicksSubject = PublishSubject.create();

    private final int action;
    private final Context context;

    public NFCDialog(@NonNull Context context, @StringRes int message, int action) {
        super(context);
        this.context = context;
        SecureUtil.markAsSecure(context, getWindow());

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogLayout = inflater.inflate(R.layout.nfc_dialog, this.getListView());

        messageView = dialogLayout.findViewById(R.id.nfcDialogText);
        iconView = dialogLayout.findViewById(R.id.nfcDialogIcon);

        messageView.setText(context.getString(message));
        setView(dialogLayout);

        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), this);

        this.action = action;
    }

    public void showStatus(NFCResponse response) {
        if (response.status() != SessionStatusResponse.ProcessStatus.OK) {
            Button btn = getButton(BUTTON_NEGATIVE);
            if (btn != null) btn.setText(android.R.string.ok);
        } else {
            Button btn = getButton(BUTTON_NEGATIVE);
            if (btn != null) btn.setText(android.R.string.cancel);
        }
        if (response.message() != null) {
            if (response.status() == SessionStatusResponse.ProcessStatus.TECHNICAL_ERROR) {
                messageView.setText(getContext().getString(R.string.signature_update_nfc_technical_error) + ":\n" + response.message());
                iconView.setColorFilter(ContextCompat.getColor(context, R.color.error), PorterDuff.Mode.SRC_IN);
            } else {
                messageView.setText(response.message());
                iconView.setColorFilter(ContextCompat.getColor(context, R.color.accent), PorterDuff.Mode.SRC_IN);
            }
        } else {
            if (AccessibilityUtils.isTalkBackEnabled()) {
                AccessibilityUtils.interrupt(getContext());
            }
            messageView.setText(getContext().getString(R.string.signature_update_nfc_hold));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button cancelButton = getButton(BUTTON_NEGATIVE);
        cancelButton.setContentDescription(getDialogActionCancelDescription());
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

    private String getDialogActionCancelDescription() {
            return "Cancel";
    }
}
