package ee.ria.DigiDoc.android.utils.rxbinding.app;

import android.content.DialogInterface;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class ObservableDialogClickListener extends Observable<Integer>
        implements DialogInterface.OnClickListener {

    private final Subject<Integer> subject = PublishSubject.create();

    @Override
    protected void subscribeActual(Observer<? super Integer> observer) {
        subject.subscribe(observer);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        subject.onNext(which);
    }
}
