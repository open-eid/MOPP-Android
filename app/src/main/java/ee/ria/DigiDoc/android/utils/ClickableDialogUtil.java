package ee.ria.DigiDoc.android.utils;

import android.app.Dialog;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public final class ClickableDialogUtil {

    public static <T> void makeLinksInDialogClickable(T dialog) {
        if (dialog instanceof Dialog) {
            Dialog errorDialog = (Dialog) dialog;
            TextView textView = errorDialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}
