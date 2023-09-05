package ee.ria.DigiDoc.android.main.settings.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

public interface Intent extends MviIntent {

    @AutoValue
    class ChooseFileIntent implements Intent {

        public static ChooseFileIntent clear() {
            return null;
        }

        public ChooseFileIntent create() {
            return new AutoValue_Intent_ChooseFileIntent();
        }
    }

    @AutoValue
    class OpenAccessIntent implements Intent {

        public static OpenAccessIntent clear() {
            return null;
        }

        public OpenAccessIntent create() {
            return new AutoValue_Intent_OpenAccessIntent();
        }
    }
}
