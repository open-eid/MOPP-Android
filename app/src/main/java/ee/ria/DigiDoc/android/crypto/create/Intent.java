package ee.ria.DigiDoc.android.crypto.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class RecipientsAddButtonClickIntent implements Intent {

        static RecipientsAddButtonClickIntent create() {
            return new AutoValue_Intent_RecipientsAddButtonClickIntent();
        }
    }
}
