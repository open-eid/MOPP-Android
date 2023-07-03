package ee.ria.DigiDoc.android.main.settings.create;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ChooseFileResult implements Result {

        @Nullable abstract Context context();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .context(context())
                    .build();
        }

        static ChooseFileResult create(@Nullable Context context) {
            return new AutoValue_Result_ChooseFileResult(context);
        }
    }
}
