package ee.ria.DigiDoc.android.signature.create;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class ChooseFilesAction implements Action {

        static ChooseFilesAction create() {
            return new AutoValue_Action_ChooseFilesAction();
        }
    }

    @AutoValue
    abstract class CreateContainerAction implements Action {

        abstract ImmutableList<FileStream> fileStreams();

        static CreateContainerAction create(ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Action_CreateContainerAction(fileStreams);
        }
    }
}
