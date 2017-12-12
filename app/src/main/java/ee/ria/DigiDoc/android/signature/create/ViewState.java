package ee.ria.DigiDoc.android.signature.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract boolean chooseFiles();

    abstract boolean createContainerInProgress();

    @Nullable abstract File containerFile();

    @Nullable abstract Throwable error();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .chooseFiles(false)
                .createContainerInProgress(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder chooseFiles(boolean chooseFiles);
        Builder createContainerInProgress(boolean createContainerInProgress);
        Builder containerFile(@Nullable File containerFile);
        Builder error(@Nullable Throwable error);
        ViewState build();
    }
}
