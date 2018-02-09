package ee.ria.DigiDoc.android.signature.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.signature.data.ContainerAdd;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class CreateContainerResult implements Result {

        abstract boolean isExistingContainer();

        @Nullable abstract File containerFile();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        @Nullable
        public Transaction transaction() {
            if (containerFile() != null) {
                return Transaction.replace(SignatureUpdateScreen
                        .create(isExistingContainer(), containerFile()));
            } else if (error() != null) {
                return Transaction.pop();
            }
            return null;
        }

        static CreateContainerResult inProgress() {
            return new AutoValue_Result_CreateContainerResult(false, null, null);
        }

        static CreateContainerResult success(ContainerAdd containerAdd) {
            return new AutoValue_Result_CreateContainerResult(containerAdd.isExistingContainer(),
                    containerAdd.containerFile(), null);
        }

        static CreateContainerResult failure(Throwable error) {
            return new AutoValue_Result_CreateContainerResult(false, null, error);
        }
    }
}
