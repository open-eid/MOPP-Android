package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    @IdRes abstract int viewId();

    abstract boolean menuOpen();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .viewId(R.id.mainHomeSignature)
                .menuOpen(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder viewId(@IdRes int viewId);
        Builder menuOpen(boolean menuOpen);
        ViewState build();
    }
}
