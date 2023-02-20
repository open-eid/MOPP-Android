package ee.ria.DigiDoc.android.utils.container;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.widget.Button;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.files.FileAlreadyExistsException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import static com.jakewharton.rxbinding4.view.RxView.clicks;

public final class NameUpdateDialog extends AlertDialog {

    private final NameUpdateView view;
    private final Subject<Object> positiveButtonClicksSubject = PublishSubject.create();

    public NameUpdateDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(context, getWindow());
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        view = new NameUpdateView(getContext());
        view.setId(R.id.nameUpdateNameLabel);
        setView(view, padding, (int)(padding / 1.2), padding, padding / 2);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok),
                (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> cancel());
    }

    public void render(boolean showing, @Nullable String name, @Nullable Throwable error) {
        if (name != null) {
            view.name(name);
        }
        view.setEnabled(showing);
        if (showing) {
            show();
            Integer errorRes;
            if (error == null) {
                errorRes = null;
            } else if (error instanceof FileAlreadyExistsException) {
                errorRes = R.string.signature_update_name_update_error_exists;
            } else {
                errorRes = R.string.signature_update_name_update_error_invalid;
            }
            view.error(errorRes);
        } else {
            dismiss();
        }
    }

    public Observable<String> updates() {
        return positiveButtonClicksSubject.map(ignored -> view.name());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button confirmButton = getButton(BUTTON_POSITIVE);
        confirmButton.setContentDescription(getContext().getString(R.string.confirm_container_name_change));
        Button cancelButton = getButton(BUTTON_NEGATIVE);
        cancelButton.setContentDescription(getContext().getString(R.string.cancel_container_name_change));
        // override default click listener to prevent dialog dismiss
        clicks(getButton(BUTTON_POSITIVE)).subscribe(positiveButtonClicksSubject);
    }
}
