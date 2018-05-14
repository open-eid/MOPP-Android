package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.files.FileAlreadyExistsException;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class NameUpdateDialog extends AlertDialog {

    private final NameUpdateView view;
    private final Subject<Object> positiveButtonClicksSubject = PublishSubject.create();

    NameUpdateDialog(@NonNull Context context) {
        super(context);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        view = new NameUpdateView(getContext());
        view.setId(R.id.signatureUpdateNameUpdateNameLabel);
        setView(view, padding, padding, padding, padding);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok),
                (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> cancel());
    }

    public void render(boolean showing, @Nullable String name, @Nullable Throwable error,
                       @Nullable SignedContainer container) {
        if (name != null) {
            view.name(name);
        }
        if (showing && container != null) {
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
        // override default click listener to prevent dialog dismiss
        clicks(getButton(BUTTON_POSITIVE)).subscribe(positiveButtonClicksSubject);
    }
}
