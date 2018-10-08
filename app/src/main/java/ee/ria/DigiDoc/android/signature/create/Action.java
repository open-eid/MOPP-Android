package ee.ria.DigiDoc.android.signature.create;

import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.navigator.TransactionAction;

import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_CREATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;

interface Action extends MviAction {

    @AutoValue
    abstract class ChooseFilesAction implements Action,
            TransactionAction<Transaction.ActivityForResultTransaction> {

        @Nullable abstract android.content.Intent intent();

        static ChooseFilesAction create(@Nullable Intent intent) {
            return new AutoValue_Action_ChooseFilesAction(Transaction.activityForResult(
                    RC_SIGNATURE_CREATE_DOCUMENTS_ADD, createGetContentIntent(), null), intent);
        }
    }
}
