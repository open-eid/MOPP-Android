package ee.ria.DigiDoc.android.signature.create;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class CreateContainerIntent implements Intent {

        abstract ImmutableList<FileStream> fileStreams();

        static CreateContainerIntent create(ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Intent_CreateContainerIntent(fileStreams);
        }
    }
}
