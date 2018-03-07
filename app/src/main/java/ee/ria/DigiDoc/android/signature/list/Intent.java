package ee.ria.DigiDoc.android.signature.list;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class UpButtonIntent implements Intent {

        static UpButtonIntent create() {
            return new AutoValue_Intent_UpButtonIntent();
        }
    }

    @AutoValue
    abstract class ContainerOpenIntent implements Intent {

        abstract File containerFile();

        static ContainerOpenIntent create(File containerFile) {
            return new AutoValue_Intent_ContainerOpenIntent(containerFile);
        }
    }

    @AutoValue
    abstract class ContainerRemoveIntent implements Intent {

        @Nullable abstract File containerFile();

        abstract boolean confirmation();

        static ContainerRemoveIntent confirmation(File containerFile) {
            return create(containerFile, true);
        }

        static ContainerRemoveIntent remove(File containerFile) {
            return create(containerFile, false);
        }

        static ContainerRemoveIntent cancel() {
            return create(null, false);
        }

        private static ContainerRemoveIntent create(@Nullable File containerFile,
                                                    boolean confirmation) {
            return new AutoValue_Intent_ContainerRemoveIntent(containerFile, confirmation);
        }
    }

    @AutoValue
    abstract class RefreshIntent implements Intent {

        static RefreshIntent create() {
            return new AutoValue_Intent_RefreshIntent();
        }
    }
}
