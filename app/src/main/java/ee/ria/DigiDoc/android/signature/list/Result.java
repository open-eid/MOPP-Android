package ee.ria.DigiDoc.android.signature.list;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class ContainersLoadResult implements Result {

        abstract boolean indicateActivity();

        abstract boolean inProgress();

        @Nullable abstract ImmutableList<File> containerFiles();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .indicateActivity(indicateActivity())
                    .containerLoadProgress(inProgress());
            if (containerFiles() != null) {
                builder.containerFiles(containerFiles());
            }
            return builder.build();
        }

        static ContainersLoadResult progress(boolean indicateActivity) {
            return create(indicateActivity, true, null, null);
        }

        static ContainersLoadResult success(ImmutableList<File> containerFiles) {
            return create(true, false, containerFiles, null);
        }

        static ContainersLoadResult failure(Throwable error) {
            return create(true, false, null, error);
        }

        private static ContainersLoadResult create(boolean indicateActivity, boolean inProgress,
                                                   @Nullable ImmutableList<File> containerFiles,
                                                   @Nullable Throwable error) {
            return new AutoValue_Result_ContainersLoadResult(indicateActivity, inProgress,
                    containerFiles, error);
        }
    }

    @AutoValue
    abstract class VoidResult implements Result {

        @Nullable abstract File confirmationContainerFile();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .sivaConfirmationContainerFile(confirmationContainerFile());
            return builder.build();
        }

        static VoidResult confirmation(File confirmationContainerFile) {
            return create(confirmationContainerFile);
        }

        static VoidResult success() {
            return create(null);
        }

        static VoidResult cancel() {
            return create(null);
        }

        static VoidResult create(@Nullable File confirmationContainerFile) {
            return new AutoValue_Result_VoidResult(confirmationContainerFile);
        }
    }

    @AutoValue
    abstract class ContainerRemoveResult implements Result {

        @Nullable abstract File confirmationContainerFile();

        abstract boolean inProgress();

        @Nullable abstract ImmutableList<File> containerFiles();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            ViewState.Builder builder = state.buildWith()
                    .removeConfirmationContainerFile(confirmationContainerFile())
                    .containerRemoveProgress(inProgress());
            if (containerFiles() != null) {
                builder.containerFiles(containerFiles());
            }
            return builder.build();
        }

        static ContainerRemoveResult confirmation(File confirmationContainerFile) {
            return create(confirmationContainerFile, false, null, null);
        }

        static ContainerRemoveResult progress() {
            return create(null, true, null, null);
        }

        static ContainerRemoveResult success(ImmutableList<File> containerFiles) {
            return create(null, false, containerFiles, null);
        }

        static ContainerRemoveResult failure(Throwable error) {
            return create(null, false, null, error);
        }

        static ContainerRemoveResult cancel() {
            return create(null, false, null, null);
        }

        private static ContainerRemoveResult create(@Nullable File confirmationContainerFile,
                                                    boolean inProgress,
                                                    @Nullable ImmutableList<File> containerFiles,
                                                    @Nullable Throwable error) {
            return new AutoValue_Result_ContainerRemoveResult(confirmationContainerFile, inProgress,
                    containerFiles, error);
        }
    }
}
