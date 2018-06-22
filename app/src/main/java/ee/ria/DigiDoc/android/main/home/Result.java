package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class InitialResult implements Result {

        @IdRes abstract int viewId();

        @IdRes @Nullable abstract Integer locale();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .viewId(viewId())
                    .locale(locale())
                    .build();
        }

        static InitialResult create(@IdRes int viewId, @IdRes @Nullable Integer locale) {
            return new AutoValue_Result_InitialResult(viewId, locale);
        }
    }

    @AutoValue
    abstract class NavigationResult implements Result {

        @IdRes abstract int viewId();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .viewId(viewId())
                    .build();
        }

        static NavigationResult create(@IdRes int viewId) {
            return new AutoValue_Result_NavigationResult(viewId);
        }
    }

    @AutoValue
    abstract class MenuResult implements Result {

        abstract boolean isOpen();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .menuOpen(isOpen())
                    .build();
        }

        static MenuResult create(boolean isOpen) {
            return new AutoValue_Result_MenuResult(isOpen);
        }
    }

    @AutoValue
    abstract class NavigationVisibilityResult implements Result {

        abstract boolean visible();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().navigationVisible(visible()).build();
        }

        static NavigationVisibilityResult create(boolean visible) {
            return new AutoValue_Result_NavigationVisibilityResult(visible);
        }
    }

    @AutoValue
    abstract class LocaleChangeResult implements Result {

        @IdRes @Nullable abstract Integer locale();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith().locale(locale()).build();
        }

        static LocaleChangeResult create(@Nullable Integer locale) {
            return new AutoValue_Result_LocaleChangeResult(locale);
        }
    }
}
