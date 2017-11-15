package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface HomeIntent extends MviIntent {

    @AutoValue
    abstract class NavigationIntent implements HomeIntent {

        @IdRes abstract int navigationItem();

        static NavigationIntent create(@IdRes int navigationItem) {
            return new AutoValue_HomeIntent_NavigationIntent(navigationItem);
        }
    }
}
