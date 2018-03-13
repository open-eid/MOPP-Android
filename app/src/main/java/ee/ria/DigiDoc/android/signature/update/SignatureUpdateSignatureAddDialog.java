package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import ee.ria.DigiDoc.R;

public final class SignatureUpdateSignatureAddDialog extends AlertDialog implements
        DialogInterface.OnClickListener {

    private final SignatureUpdateSignatureAddView view;

    public SignatureUpdateSignatureAddDialog(@NonNull Context context) {
        super(context);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        view = new SignatureUpdateSignatureAddView(getContext());
        view.setId(R.id.signatureUpdateSignatureAdd);
        setView(view, padding, padding, padding, padding);
        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
    }

    public SignatureUpdateSignatureAddView getView() {
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }
}
