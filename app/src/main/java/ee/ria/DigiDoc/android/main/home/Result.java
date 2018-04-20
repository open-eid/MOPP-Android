package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

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
}
