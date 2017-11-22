package ee.ria.DigiDoc.android.utils;

import android.support.annotation.Nullable;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public final class ViewDisposables {

    @Nullable private CompositeDisposable compositeDisposable;

    public void attach() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    public void detach() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
    }

    public void add(Disposable d) {
        if (compositeDisposable == null) {
            throw new IllegalStateException("Can't add disposables when not attached.");
        }
        compositeDisposable.add(d);
    }
}
