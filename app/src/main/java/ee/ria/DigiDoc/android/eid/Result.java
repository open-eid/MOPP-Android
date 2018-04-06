package ee.ria.DigiDoc.android.eid;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class LoadResult implements Result {

        abstract IdCardDataResponse idCardDataResponse();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .idCardDataResponse(idCardDataResponse())
                    .build();
        }

        static LoadResult create(IdCardDataResponse idCardDataResponse) {
            return new AutoValue_Result_LoadResult(idCardDataResponse);
        }
    }
}
