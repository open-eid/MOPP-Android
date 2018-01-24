package ee.ria.DigiDoc.android.main.home;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.navigation.Screen;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class NavigationResult implements Result {

        abstract Screen screen();

        @Override
        public ViewState reduce(ViewState state) {
            return ViewState.create(screen());
        }

        static NavigationResult create(Screen screen) {
            return new AutoValue_Result_NavigationResult(screen);
        }
    }
}
