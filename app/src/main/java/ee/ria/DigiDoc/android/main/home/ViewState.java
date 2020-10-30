package ee.ria.DigiDoc.android.main.home;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    @IdRes abstract int viewId();

    abstract boolean menuOpen();

    abstract boolean navigationVisible();

    @Nullable abstract Integer locale();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .viewId(R.id.mainHomeSignature)
                .menuOpen(false)
                .navigationVisible(true)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder viewId(@IdRes int viewId);
        Builder menuOpen(boolean menuOpen);
        Builder navigationVisible(boolean navigationVisible);
        Builder locale(@Nullable Integer locale);
        ViewState build();
    }
}
