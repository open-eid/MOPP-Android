package ee.ria.DigiDoc.android.main.settings.create;

import static ee.ria.DigiDoc.android.Constants.RC_TSA_CERT_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.navigator.TransactionAction;

interface Action extends MviAction {

    @AutoValue
    abstract class InitialAction implements Action {

        static InitialAction create() {
            return new AutoValue_Action_InitialAction();
        }
    }

    @AutoValue
    abstract class ChooseFileAction implements Action,
            TransactionAction<Transaction.ActivityForResultTransaction> {

        @Nullable abstract android.content.Intent intent();

        static ChooseFileAction create() {
            return new AutoValue_Action_ChooseFileAction(Transaction.activityForResult(
                    RC_TSA_CERT_ADD, createGetContentIntent(false), null), null);
        }
    }
}
