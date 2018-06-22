package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent, Action {

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

    @AutoValue
    abstract class MenuIntent implements Intent {

        @Nullable abstract Boolean isOpen();

        @Nullable @IdRes abstract Integer menuItem();

        static MenuIntent state(boolean isOpen) {
            return create(isOpen, null);
        }

        static MenuIntent navigate(@IdRes int menuItem) {
            return create(null, menuItem);
        }

        private static MenuIntent create(@Nullable Boolean isOpen,
                                         @Nullable @IdRes Integer menuItem) {
            return new AutoValue_Intent_MenuIntent(isOpen, menuItem);
        }
    }

    @AutoValue
    abstract class NavigationVisibilityIntent implements Intent, Action {

        abstract boolean visible();

        static NavigationVisibilityIntent create(boolean visible) {
            return new AutoValue_Intent_NavigationVisibilityIntent(visible);
        }
    }

    @AutoValue
    abstract class LocaleChangeIntent implements Intent, Action {

        @IdRes abstract int item();

        static LocaleChangeIntent create(@IdRes int item) {
            return new AutoValue_Intent_LocaleChangeIntent(item);
        }
    }
}
