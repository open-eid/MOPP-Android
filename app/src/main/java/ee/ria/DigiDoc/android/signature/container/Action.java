package ee.ria.DigiDoc.android.signature.container;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.IntentUtils;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class ChooseDocumentsAction implements Action {

        static ChooseDocumentsAction create() {
            return new AutoValue_Action_ChooseDocumentsAction();
        }
    }

    @AutoValue
    abstract class AddDocumentsAction implements Action {

        abstract ImmutableList<IntentUtils.FileStream> fileStreams();

        static AddDocumentsAction create(ImmutableList<IntentUtils.FileStream> fileStreams) {
            return new AutoValue_Action_AddDocumentsAction(fileStreams);
        }
    }
}
