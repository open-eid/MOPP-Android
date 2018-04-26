package ee.ria.DigiDoc.android.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public final class InputMethodUtils {

    public static void hideSoftKeyboard(View view) {
        //noinspection ConstantConditions
        ((InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
