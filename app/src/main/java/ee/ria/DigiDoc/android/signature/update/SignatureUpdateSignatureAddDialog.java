package ee.ria.DigiDoc.android.signature.update;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static ee.ria.DigiDoc.android.Constants.VOID;
import static ee.ria.DigiDoc.android.utils.display.DisplayUtil.getDeviceLayoutWidth;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.rxbinding.app.ObservableDialogClickListener;
import io.reactivex.rxjava3.core.Observable;

public final class SignatureUpdateSignatureAddDialog extends AlertDialog {

    private final SignatureUpdateSignatureAddView view;
    private final ObservableDialogClickListener positiveButtonClicks;
    private View.OnLayoutChangeListener layoutChangeListener;

    private final ViewDisposables disposables = new ViewDisposables();

    private final Button mobileIdPositiveButton;
    private final Button mobileIdCancelButton;
    private final Button smartIdPositiveButton;
    private final Button smartIdCancelButton;
    private final Button idCardPositiveButton;
    private final Button idCardCancelButton;

    SignatureUpdateSignatureAddDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(context, getWindow());
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        view = new SignatureUpdateSignatureAddView(getContext());
        view.setId(R.id.signatureUpdateSignatureAdd);
        setView(view, padding, padding, padding, padding);

        positiveButtonClicks = new ObservableDialogClickListener();

        // Buttons to show when in portrait mode
        setButton(BUTTON_POSITIVE,
                getContext().getString(R.string.signature_update_signature_add_positive_button),
                positiveButtonClicks);
        Button signButton = getButton(BUTTON_POSITIVE);
        if (signButton != null) {
            String signButtonText = signButton.getText() != null ?
                    signButton.getText().toString().toLowerCase() : null;
            signButton.setContentDescription(signButtonText != null ? signButtonText :
                    getContext().getString(R.string.signature_update_signature_add_positive_button));
        }

        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> {
                    cancel();
                    AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.signing_cancelled);
                }
        );

        // Buttons to show when in landscape mode (to give more room for TextViews)
        mobileIdPositiveButton = view.findViewById(R.id.signatureUpdateMobileIdSignButton);
        mobileIdCancelButton = view.findViewById(R.id.signatureUpdateMobileIdCancelSigningButton);
        setLandscapeButtons(getContext(), mobileIdPositiveButton, mobileIdCancelButton, positiveButtonClicks);

        smartIdPositiveButton = view.findViewById(R.id.signatureUpdateSmartIdSignButton);
        smartIdCancelButton = view.findViewById(R.id.signatureUpdateSmartIdCancelSigningButton);
        setLandscapeButtons(getContext(), smartIdPositiveButton, smartIdCancelButton, positiveButtonClicks);

        idCardPositiveButton = view.findViewById(R.id.signatureUpdateIdCardSignButton);
        idCardCancelButton = view.findViewById(R.id.signatureUpdateIdCardCancelButton);
        setLandscapeButtons(getContext(), idCardPositiveButton, idCardCancelButton, positiveButtonClicks);
    }

    public SignatureUpdateSignatureAddView view() {
        return view;
    }

    Observable<Object> positiveButtonClicks() {
        return positiveButtonClicks.map(ignored -> VOID);
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window != null) {
            // https://stackoverflow.com/questions/9102074/android-edittext-in-dialog-doesnt-pull-up-soft-keyboard
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            setCustomLayoutChangeListener(window);
            view.addOnLayoutChangeListener(getCustomLayoutChangeListener());
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Button confirmButton = getButton(BUTTON_POSITIVE);
        confirmButton.setContentDescription(getContext().getString(R.string.sign_container));
        Button cancelButton = getButton(BUTTON_NEGATIVE);
        cancelButton.setContentDescription(getContext().getString(R.string.cancel_signing_process));
        setButtonsBasedOnOrientation(getContext().getResources().getConfiguration().orientation);
        mobileIdCancelButton.setTextColor(ContextCompat.getColor(getContext(), R.color.accent));
        smartIdCancelButton.setTextColor(ContextCompat.getColor(getContext(), R.color.accent));
        idCardCancelButton.setTextColor(ContextCompat.getColor(getContext(), R.color.accent));
        disposables.attach();
        disposables.add(view.positiveButtonEnabled().subscribe(enabled -> {
            Button positiveButton = getButton(BUTTON_POSITIVE);
            Button[] additionalPositiveButtons = { mobileIdPositiveButton, smartIdPositiveButton, idCardPositiveButton };
            updateButtonStateAndColor(positiveButton, additionalPositiveButtons, enabled);
        }));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        removeListeners();
        super.onDetachedFromWindow();
    }

    // Prevent Dialog width change when rotating screen
    private void setCustomLayoutChangeListener(Window window) {
        layoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                window.setLayout(getDeviceLayoutWidth(getContext()), WRAP_CONTENT);
    }

    private View.OnLayoutChangeListener getCustomLayoutChangeListener() {
        return layoutChangeListener;
    }

    private void removeListeners() {
        if (layoutChangeListener == null) { return; }
        view.removeOnLayoutChangeListener(layoutChangeListener);
        layoutChangeListener = null;
    }

    void setButtonsBasedOnOrientation(int orientation) {
        Button confirmButton = getButton(BUTTON_POSITIVE);
        Button cancelButton = getButton(BUTTON_NEGATIVE);

        if (confirmButton != null && cancelButton != null) {
            confirmButton.setVisibility(orientation == Configuration.ORIENTATION_PORTRAIT ? VISIBLE : GONE);
            cancelButton.setVisibility(orientation == Configuration.ORIENTATION_PORTRAIT ? VISIBLE : GONE);
        }

        if (mobileIdPositiveButton != null && mobileIdCancelButton != null &&
                smartIdPositiveButton != null && smartIdCancelButton != null &&
                idCardPositiveButton != null && idCardCancelButton != null) {
            int visibility = orientation == Configuration.ORIENTATION_LANDSCAPE ? VISIBLE : GONE;
            mobileIdPositiveButton.setVisibility(visibility);
            mobileIdCancelButton.setVisibility(visibility);
            smartIdPositiveButton.setVisibility(visibility);
            smartIdCancelButton.setVisibility(visibility);
            idCardPositiveButton.setVisibility(visibility);
            idCardCancelButton.setVisibility(visibility);
        }
    }

    private void setLandscapeButtons(Context context, Button positiveButton, Button cancelButton, ObservableDialogClickListener clickListener) {
        positiveButton.setText(getContext().getString(R.string.signature_update_signature_add_positive_button));
        positiveButton.setOnClickListener(v -> clickListener.onClick(this, DialogInterface.BUTTON_POSITIVE));

        cancelButton.setText(getContext().getString(android.R.string.cancel));
        cancelButton.setOnClickListener(v -> {
            cancel();
            AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.signing_cancelled);
        });
    }

    private void updateButtonStateAndColor(Button positiveButton, Button[] additionalPositiveButtons, boolean enabled) {
        int textColor = enabled ? ContextCompat.getColor(getContext(), R.color.accent) : Color.GRAY;

        for (Button button : additionalPositiveButtons) {
            if (button != null) {
                button.setEnabled(enabled);
                button.setTextColor(textColor);
            }
        }

        if (positiveButton != null) {
            positiveButton.setEnabled(enabled);
            positiveButton.setTextColor(textColor);
        }
    }
}
