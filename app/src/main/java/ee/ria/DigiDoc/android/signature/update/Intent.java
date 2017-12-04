package ee.ria.DigiDoc.android.signature.update;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        abstract File containerFile();

        static InitialIntent create(File containerFile) {
            return new AutoValue_Intent_InitialIntent(containerFile);
        }
    }
}
