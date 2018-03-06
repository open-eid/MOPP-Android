package ee.ria.DigiDoc.android.main.home;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class NavigationResult implements Result {

        abstract Screen screen();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .screen(screen())
                    .build();
        }

        static NavigationResult create(Screen screen) {
            return new AutoValue_Result_NavigationResult(screen);
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
