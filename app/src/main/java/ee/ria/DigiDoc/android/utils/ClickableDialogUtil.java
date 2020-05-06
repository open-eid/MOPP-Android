package ee.ria.DigiDoc.android.utils;

import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;

public final class ClickableDialogUtil {

    public static <T> void makeLinksInDialogClickable(T dialog) {
        if (dialog instanceof ErrorDialog) {
            ErrorDialog errorDialog = (ErrorDialog) dialog;
            TextView textView = errorDialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

}
