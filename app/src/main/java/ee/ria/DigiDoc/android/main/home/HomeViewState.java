package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class HomeViewState implements MviViewState {

    @IdRes abstract int currentScreen();

    static HomeViewState create(@IdRes int currentScreen) {
        return new AutoValue_HomeViewState(currentScreen);
    }
}
