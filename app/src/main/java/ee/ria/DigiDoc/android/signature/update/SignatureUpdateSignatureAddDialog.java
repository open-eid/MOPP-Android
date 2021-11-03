package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.rxbinding.app.ObservableDialogClickListener;
import io.reactivex.rxjava3.core.Observable;

import static ee.ria.DigiDoc.android.Constants.VOID;

public final class SignatureUpdateSignatureAddDialog extends AlertDialog {

    private final SignatureUpdateSignatureAddView view;
    private final ObservableDialogClickListener positiveButtonClicks;

    private final ViewDisposables disposables = new ViewDisposables();

    SignatureUpdateSignatureAddDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(getWindow());
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        view = new SignatureUpdateSignatureAddView(getContext());
        view.setId(R.id.signatureUpdateSignatureAdd);
        setView(view, padding, padding, padding, padding);
        setButton(BUTTON_POSITIVE,
                getContext().getString(R.string.signature_update_signature_add_positive_button),
                positiveButtonClicks = new ObservableDialogClickListener());
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> {
                    cancel();
                    AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.container_signing_cancelled);
                }
        );
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
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Button confirmButton = getButton(BUTTON_POSITIVE);
        confirmButton.setContentDescription(getContext().getString(R.string.sign_container));
        Button cancelButton = getButton(BUTTON_NEGATIVE);
        cancelButton.setContentDescription(getContext().getString(R.string.cancel_signing_process));
        disposables.attach();
        disposables.add(view.positiveButtonEnabled().subscribe(enabled ->
                getButton(BUTTON_POSITIVE).setEnabled(enabled)));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
