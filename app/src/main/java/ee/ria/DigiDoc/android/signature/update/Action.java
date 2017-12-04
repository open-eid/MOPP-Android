package ee.ria.DigiDoc.android.signature.update;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class LoadContainerAction implements Action {

        abstract File containerFile();

        static LoadContainerAction create(File containerFile) {
            return new AutoValue_Action_LoadContainerAction(containerFile);
        }
    }
}
