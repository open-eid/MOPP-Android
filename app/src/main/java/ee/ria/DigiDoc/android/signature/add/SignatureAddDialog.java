package ee.ria.DigiDoc.android.signature.add;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static ee.ria.DigiDoc.android.Constants.VOID;

public final class SignatureAddDialog extends AlertDialog implements
        DialogInterface.OnClickListener {

    private final SignatureAddView signatureAddView;

    private final Subject<Integer> buttonClicksSubject = PublishSubject.create();

    public SignatureAddDialog(@NonNull Context context, String phoneNo, String personalCode) {
        super(context);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        setTitle(R.string.signature_add_title);
        signatureAddView = new SignatureAddView(getContext());
        signatureAddView.setId(R.id.signatureAdd);
        signatureAddView.setPhoneNo(phoneNo);
        signatureAddView.setPersonalCode(personalCode);
        setView(signatureAddView, padding, padding, padding, padding);
        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        buttonClicksSubject.onNext(which);
    }

    public Observable<Object> cancels() {
        return RxDialog.cancels(this)
                .mergeWith(buttonClicksSubject
                        .filter(which -> which == BUTTON_NEGATIVE)
                        .map(ignored -> VOID));
    }

    public Observable<SignatureAddView.Data> positiveButtonClicks() {
        return buttonClicksSubject
                .filter(which -> which == BUTTON_POSITIVE)
                .map(ignored -> signatureAddView.getData());
    }
}
