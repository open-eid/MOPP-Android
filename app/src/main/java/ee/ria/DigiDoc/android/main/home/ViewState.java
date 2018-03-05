package ee.ria.DigiDoc.android.main.home;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.home.SignatureHomeScreen;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract Screen screen();

    abstract boolean menuOpen();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .screen(SignatureHomeScreen.create())
                .menuOpen(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder screen(Screen screen);
        Builder menuOpen(boolean menuOpen);
        ViewState build();
    }
}
