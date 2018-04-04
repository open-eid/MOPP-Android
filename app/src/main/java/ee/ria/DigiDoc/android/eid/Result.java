package ee.ria.DigiDoc.android.eid;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class LoadResult implements Result {

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static LoadResult create() {
            return new AutoValue_Result_LoadResult();
        }
    }
}
