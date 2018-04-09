package ee.ria.DigiDoc.android.eid;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class LoadIntent implements Intent {

        static LoadIntent create() {
            return new AutoValue_Intent_LoadIntent();
        }
    }
}
