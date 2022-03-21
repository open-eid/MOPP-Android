package ee.ria.DigiDoc.android.signature.update;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.common.RoleData;
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

        abstract boolean isExistingContainer();

        static ContainerLoadAction create(File containerFile, @Nullable Integer signatureAddMethod,
                                          boolean signatureAddSuccessMessageVisible, boolean isExistingContainer) {
            return new AutoValue_Action_ContainerLoadAction(containerFile, signatureAddMethod,
                    signatureAddSuccessMessageVisible, isExistingContainer);
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
    abstract class RoleDetailsAction implements Action {

        abstract Signature signature();

        static RoleDetailsAction create(Signature signature) {
            return new AutoValue_Action_RoleDetailsAction(signature);
        }
    }

    @AutoValue
    abstract class DocumentRemoveAction implements Action {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        abstract ImmutableList<DataFile> documents();

        @Nullable abstract DataFile document();

        static DocumentRemoveAction create(boolean showConfirmation, @Nullable File containerFile,
                                           ImmutableList<DataFile> documents, @Nullable DataFile document) {
            return new AutoValue_Action_DocumentRemoveAction(showConfirmation, containerFile, documents, document);
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
    abstract class SignatureViewAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract Signature signature();

        static SignatureViewAction create(@Nullable File containerFile,
                                         @Nullable Signature signature) {
            return new AutoValue_Action_SignatureViewAction(containerFile,
                    signature);
        }
    }

    @AutoValue
    abstract class SignatureAddAction implements Action {

        @Nullable abstract Integer method();

        @Nullable abstract Boolean existingContainer();

        @Nullable abstract File containerFile();

        @Nullable abstract SignatureAddRequest request();

        abstract boolean isCancelled();

        @Nullable abstract Boolean showRoleAddingView();

        @Nullable abstract RoleData roleData();

        static SignatureAddAction create(@Nullable Integer method,
                                         @Nullable Boolean existingContainer,
                                         @Nullable File containerFile,
                                         @Nullable SignatureAddRequest request,
                                         boolean isCancelled,
                                         @Nullable Boolean showRoleAddingView,
                                         @Nullable RoleData roleData) {
            return new AutoValue_Action_SignatureAddAction(method, existingContainer, containerFile,
                    request, isCancelled, showRoleAddingView, roleData);
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
