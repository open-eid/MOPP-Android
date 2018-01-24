package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class NavigationIntent implements Intent {

        @IdRes abstract int item();

        static NavigationIntent create(@IdRes int item) {
            return new AutoValue_Intent_NavigationIntent(item);
        }
    }
}
