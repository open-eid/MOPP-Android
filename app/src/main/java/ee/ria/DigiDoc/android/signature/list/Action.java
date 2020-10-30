package ee.ria.DigiDoc.android.signature.list;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.navigator.TransactionAction;

interface Action extends MviAction {

    @AutoValue
    abstract class ContainersLoadAction implements Action {

        abstract boolean indicateActivity();

        static ContainersLoadAction create(boolean indicateActivity) {
            return new AutoValue_Action_ContainersLoadAction(indicateActivity);
        }
    }

    @AutoValue
    abstract class NavigateUpAction implements Action,
            TransactionAction<Transaction.PopTransaction> {

        static NavigateUpAction create() {
            return new AutoValue_Action_NavigateUpAction(Transaction.pop());
        }
    }

    @AutoValue
    abstract class ContainerRemoveAction implements Action {

        @Nullable abstract File containerFile();

        abstract boolean confirmation();

        static ContainerRemoveAction create(@Nullable File containerFile, boolean confirmation) {
            return new AutoValue_Action_ContainerRemoveAction(containerFile, confirmation);
        }
    }
}
