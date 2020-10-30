package ee.ria.DigiDoc.android.signature.list;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
abstract class ViewState implements MviViewState {

    abstract boolean indicateActivity();

    abstract boolean containerLoadProgress();

    abstract ImmutableList<File> containerFiles();

    @Nullable abstract File removeConfirmationContainerFile();

    abstract boolean containerRemoveProgress();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .indicateActivity(true)
                .containerLoadProgress(false)
                .containerFiles(ImmutableList.of())
                .containerRemoveProgress(false)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder indicateActivity(boolean indicateActivity);
        Builder containerLoadProgress(boolean containerLoadProgress);
        Builder containerFiles(ImmutableList<File> containerFiles);
        Builder removeConfirmationContainerFile(@Nullable File removeConfirmationContainerFile);
        Builder containerRemoveProgress(boolean containerRemoveProgress);
        ViewState build();
    }
}
