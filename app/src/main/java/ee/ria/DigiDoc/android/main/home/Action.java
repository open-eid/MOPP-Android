package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class NavigationAction implements Action {

        @IdRes abstract int item();

        static NavigationAction create(@IdRes int item) {
            return new AutoValue_Action_NavigationAction(item);
        }
    }
}
