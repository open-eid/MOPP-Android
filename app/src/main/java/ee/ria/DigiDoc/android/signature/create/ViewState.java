package ee.ria.DigiDoc.android.signature.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    static ViewState initial() {
        return new AutoValue_ViewState();
    }
}
