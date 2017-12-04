package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class LoadContainerResult implements Result {

        abstract boolean inProgress();

        @Nullable abstract SignatureContainer container();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state
                    .buildWith()
                    .loadContainerInProgress(inProgress())
                    .container(container())
                    .loadContainerError(error())
                    .build();
        }

        static LoadContainerResult progress() {
            return new AutoValue_Result_LoadContainerResult(true, null, null);
        }

        static LoadContainerResult success(SignatureContainer container) {
            return new AutoValue_Result_LoadContainerResult(false, container, null);
        }

        static LoadContainerResult failure(Throwable error) {
            return new AutoValue_Result_LoadContainerResult(false, null, error);
        }
    }
}
