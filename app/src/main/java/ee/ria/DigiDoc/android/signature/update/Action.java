package ee.ria.DigiDoc.android.signature.update;

import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_UPDATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.navigator.TransactionAction;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;

interface Action extends MviAction {

    @AutoValue
    abstract class ContainerLoadAction implements Action {

        abstract File containerFile();

        @Nullable abstract Integer signatureAddMethod();

        abstract boolean signatureAddSuccessMessageVisible();

        abstract boolean isExistingContainer();

        abstract boolean isSivaConfirmed();

        static ContainerLoadAction create(File containerFile, @Nullable Integer signatureAddMethod,
                                          boolean signatureAddSuccessMessageVisible, boolean isExistingContainer,
                                          boolean isSivaConfirmed) {
            return new AutoValue_Action_ContainerLoadAction(containerFile, signatureAddMethod,
                    signatureAddSuccessMessageVisible, isExistingContainer, isSivaConfirmed);
        }
    }

    @AutoValue
    abstract class DocumentsAddAction implements Action,
            TransactionAction<Transaction.ActivityForResultTransaction> {

        @Nullable abstract File containerFile();

        static DocumentsAddAction create(@Nullable File containerFile) {
            return new AutoValue_Action_DocumentsAddAction(
                    Transaction.activityForResult(RC_SIGNATURE_UPDATE_DOCUMENTS_ADD,
                            createGetContentIntent(true), null),
                    containerFile);
        }
    }

    @AutoValue
    abstract class SignatureRoleDetailsAction implements Action {

        abstract Signature signature();

        static SignatureRoleDetailsAction create(Signature signature) {
            return new AutoValue_Action_SignatureRoleDetailsAction(signature);
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

        abstract boolean isSivaConfirmed();

        static SignatureViewAction create(@Nullable File containerFile,
                                         @Nullable Signature signature,
                                          boolean isSivaConfirmed) {
            return new AutoValue_Action_SignatureViewAction(containerFile,
                    signature, isSivaConfirmed);
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

        abstract boolean isSivaConfirmed();

        static SignatureAddAction create(@Nullable Integer method,
                                         @Nullable Boolean existingContainer,
                                         @Nullable File containerFile,
                                         @Nullable SignatureAddRequest request,
                                         boolean isCancelled,
                                         @Nullable Boolean showRoleAddingView,
                                         @Nullable RoleData roleData,
                                         boolean isSivaConfirmed) {
            return new AutoValue_Action_SignatureAddAction(method, existingContainer, containerFile,
                    request, isCancelled, showRoleAddingView, roleData, isSivaConfirmed);
        }
    }

    @AutoValue
    abstract class SendAction implements Action {

        abstract File containerFile();

        static SendAction create(File containerFile) {
            return new AutoValue_Action_SendAction(containerFile);
        }
    }

    @AutoValue
    abstract class ContainerSaveAction implements Action {

        abstract File containerFile();

        static ContainerSaveAction create(File containerFile) {
            return new AutoValue_Action_ContainerSaveAction(containerFile);
        }
    }

    @AutoValue
    abstract class EncryptAction implements Action {

        abstract File containerFile();

        static EncryptAction create(File containerFile) {
            return new AutoValue_Action_EncryptAction(containerFile);
        }
    }
}
