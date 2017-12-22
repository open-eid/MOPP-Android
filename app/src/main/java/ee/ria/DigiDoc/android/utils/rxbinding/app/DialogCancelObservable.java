package ee.ria.DigiDoc.android.utils.rxbinding.app;

import android.app.Dialog;
import android.content.DialogInterface;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

import static ee.ria.DigiDoc.android.Constants.VOID;
import static io.reactivex.android.MainThreadDisposable.verifyMainThread;

final class DialogCancelObservable extends Observable<Object> {

    private final Dialog dialog;

    DialogCancelObservable(Dialog dialog) {
        this.dialog = dialog;
    }

    @Override
    protected void subscribeActual(Observer<? super Object> observer) {
        verifyMainThread();
        Listener listener = new Listener(dialog, observer);
        observer.onSubscribe(listener);
        dialog.setOnCancelListener(listener);
    }

    static final class Listener extends MainThreadDisposable implements
            DialogInterface.OnCancelListener {

        private final Dialog dialog;
        private final Observer<? super Object> observer;

        Listener(Dialog dialog, Observer<? super Object> observer) {
            this.dialog = dialog;
            this.observer = observer;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (!isDisposed()) {
                observer.onNext(VOID);
            }
        }

        @Override
        protected void onDispose() {
            dialog.setOnCancelListener(null);
        }
    }
}
