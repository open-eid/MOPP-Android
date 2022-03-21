package ee.ria.DigiDoc.android.signature.update;

import static ee.ria.DigiDoc.android.Constants.VOID;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.rxbinding.app.ObservableDialogClickListener;
import io.reactivex.rxjava3.core.Observable;

public final class RoleAddDialog extends AlertDialog {

    private final RoleAddView view;
    private final ObservableDialogClickListener positiveButtonClicks;

    private final ViewDisposables disposables = new ViewDisposables();

    RoleAddDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(context, getWindow());

        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        view = new RoleAddView(getContext());
        view.setId(R.id.signatureUpdateRoleAdd);
        setView(view, padding, padding, padding, padding);
        setButton(BUTTON_POSITIVE,
                getContext().getString(R.string.signature_update_signature_add_positive_button),
                positiveButtonClicks = new ObservableDialogClickListener());
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> {
                    cancel();
                    AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.signing_cancelled);
                }
        );
    }

    public RoleAddView view() {
        return view;
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
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    public Observable<Object> positiveButtonClicks() {
        return positiveButtonClicks.map(ignored -> VOID);
    }
}
