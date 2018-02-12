package ee.ria.DigiDoc.android.signature.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }
}
