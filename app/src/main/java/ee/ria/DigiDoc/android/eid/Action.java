package ee.ria.DigiDoc.android.eid;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class LoadAction implements Action {

        abstract boolean clear();

        static LoadAction create(boolean clear) {
            return new AutoValue_Action_LoadAction(clear);
        }
    }
}
