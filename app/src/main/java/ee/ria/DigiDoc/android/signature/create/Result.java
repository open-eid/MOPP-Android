package ee.ria.DigiDoc.android.signature.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ChooseFilesResult implements Result {

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .chooseFiles(true)
                    .build();
        }

        static ChooseFilesResult create() {
            return new AutoValue_Result_ChooseFilesResult();
        }
    }

    @AutoValue
    abstract class CreateContainerResult implements Result {

        @Nullable abstract File containerFile();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state.buildWith()
                    .chooseFiles(false)
                    .createContainerInProgress(containerFile() == null && error() == null)
                    .containerFile(containerFile())
                    .error(error())
                    .build();
        }

        static CreateContainerResult inProgress() {
            return new AutoValue_Result_CreateContainerResult(null, null);
        }

        static CreateContainerResult success(File containerFile) {
            return new AutoValue_Result_CreateContainerResult(containerFile, null);
        }

        static CreateContainerResult failure(Throwable error) {
            return new AutoValue_Result_CreateContainerResult(null, error);
        }
    }
}
