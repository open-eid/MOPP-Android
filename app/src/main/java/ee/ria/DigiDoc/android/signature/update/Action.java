package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.navigator.TransactionAction;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;

import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_UPDATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;

interface Action extends MviAction {

    @AutoValue
    abstract class ContainerLoadAction implements Action {

        abstract File containerFile();

        @Nullable abstract Integer signatureAddMethod();

        abstract boolean signatureAddSuccessMessageVisible();

        static ContainerLoadAction create(File containerFile, @Nullable Integer signatureAddMethod,
                                          boolean signatureAddSuccessMessageVisible) {
            return new AutoValue_Action_ContainerLoadAction(containerFile, signatureAddMethod,
                    signatureAddSuccessMessageVisible);
        }
    }

    @AutoValue
    abstract class DocumentsAddAction implements Action,
            TransactionAction<Transaction.ActivityForResultTransaction> {

        @Nullable abstract File containerFile();

        static DocumentsAddAction create(@Nullable File containerFile) {
            return new AutoValue_Action_DocumentsAddAction(
                    Transaction.activityForResult(RC_SIGNATURE_UPDATE_DOCUMENTS_ADD,
                            createGetContentIntent(), null),
                    containerFile);
        }
    }

    @AutoValue
    abstract class DocumentRemoveAction implements Action {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        @Nullable abstract DataFile document();

        static DocumentRemoveAction create(boolean showConfirmation, @Nullable File containerFile,
                                           @Nullable DataFile document) {
            return new AutoValue_Action_DocumentRemoveAction(showConfirmation, containerFile,
                    document);
        }
    }

    @AutoValue
    abstract class SignatureRemoveAction implements Action {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        @Nullable abstract Signature signature();

        static SignatureRemoveAction create(boolean showConfirmation, @Nullable File containerFile,
                                            @Nullable Signature signature) {
            return new AutoValue_Action_SignatureRemoveAction(showConfirmation, containerFile,
                    signature);
        }
    }

    @AutoValue
    abstract class SignatureAddAction implements Action {

        @Nullable abstract Integer method();

        @Nullable abstract Boolean existingContainer();

        @Nullable abstract File containerFile();

        @Nullable abstract SignatureAddRequest request();

        static SignatureAddAction create(@Nullable Integer method,
                                         @Nullable Boolean existingContainer,
                                         @Nullable File containerFile,
                                         @Nullable SignatureAddRequest request) {
            return new AutoValue_Action_SignatureAddAction(method, existingContainer, containerFile,
                    request);
        }
    }

    @AutoValue
    abstract class SendAction implements Action {

        abstract File containerFile();

        static SendAction create(File containerFile) {
            return new AutoValue_Action_SendAction(containerFile);
        }
    }
}
