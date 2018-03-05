package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;

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

    @AutoValue
    abstract class MenuAction implements Action {

        @Nullable abstract Boolean isOpen();

        @Nullable @IdRes abstract Integer menuItem();

        static MenuAction create(@Nullable Boolean isOpen, @Nullable @IdRes Integer menuItem) {
            return new AutoValue_Action_MenuAction(isOpen, menuItem);
        }
    }
}
