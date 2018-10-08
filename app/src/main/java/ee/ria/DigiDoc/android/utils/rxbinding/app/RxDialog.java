package ee.ria.DigiDoc.android.utils.rxbinding.app;

import android.app.Dialog;

import io.reactivex.Observable;

public final class RxDialog {

    public static Observable<Object> dismisses(Dialog dialog) {
        return new DialogDismissObservable(dialog);
    }

    public static Observable<Object> cancels(Dialog dialog) {
        return new DialogCancelObservable(dialog);
    }
}
