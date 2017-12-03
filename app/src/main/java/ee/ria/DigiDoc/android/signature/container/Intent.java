package ee.ria.DigiDoc.android.signature.container;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.IntentUtils;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class ChooseDocumentsIntent implements Intent {

        static ChooseDocumentsIntent create() {
            return new AutoValue_Intent_ChooseDocumentsIntent();
        }
    }

    @AutoValue
    abstract class AddDocumentsIntent implements Intent {

        abstract ImmutableList<IntentUtils.FileStream> fileStreams();

        static AddDocumentsIntent create(ImmutableList<IntentUtils.FileStream> fileStreams) {
            return new AutoValue_Intent_AddDocumentsIntent(fileStreams);
        }
    }
}
