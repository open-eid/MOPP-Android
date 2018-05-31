package ee.ria.DigiDoc.android.crypto.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.mvi.State;

@AutoValue
abstract class ViewState implements MviViewState {

    @State abstract String recipientsSearchState();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .recipientsSearchState(State.IDLE)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder recipientsSearchState(@State String recipientsSearchState);
        ViewState build();
    }
}
