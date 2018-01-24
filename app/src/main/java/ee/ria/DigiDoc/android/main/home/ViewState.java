package ee.ria.DigiDoc.android.main.home;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.home.SignatureHomeScreen;
import ee.ria.DigiDoc.android.utils.mvi.MviViewState;
import ee.ria.DigiDoc.android.utils.navigation.Screen;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract Screen screen();

    static ViewState create(Screen screen) {
        return new AutoValue_ViewState(screen);
    }

    static ViewState initial() {
        return create(SignatureHomeScreen.create());
    }
}
