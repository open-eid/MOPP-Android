package ee.ria.DigiDoc.android.signature.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ChooseFilesResult implements Result {

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static ChooseFilesResult create() {
            return new AutoValue_Result_ChooseFilesResult();
        }
    }
}
