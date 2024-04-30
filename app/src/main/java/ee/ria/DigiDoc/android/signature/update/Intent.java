package ee.ria.DigiDoc.android.signature.update;

import static com.google.common.io.Files.getFileExtension;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Locale;

import ee.ria.DigiDoc.android.utils.SivaUtil;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import timber.log.Timber;

interface Intent extends MviIntent {

    Action action();
}

class InitialIntent implements Intent {

    private final boolean isExistingContainer;
    private final File containerFile;
    @Nullable private final Integer signatureAddMethod;
    private final boolean signatureAddSuccessMessageVisible;
    private final boolean isSivaConfirmed;

    private InitialIntent(boolean isExistingContainer, File containerFile,
                          @Nullable Integer signatureAddMethod,
                          boolean signatureAddSuccessMessageVisible,
                          boolean isSivaConfirmed) {
        this.isExistingContainer = isExistingContainer;
        this.containerFile = containerFile;
        this.signatureAddMethod = signatureAddMethod;
        this.signatureAddSuccessMessageVisible = signatureAddSuccessMessageVisible;
        this.isSivaConfirmed = isSivaConfirmed;
    }

    public static InitialIntent create(boolean isExistingContainer, File containerFile,
                                       Integer signatureAddMethod,
                                       boolean signatureAddSuccessMessageVisible,
                                       boolean isSivaConfirmed) {
        return new InitialIntent(isExistingContainer, containerFile, signatureAddMethod, signatureAddSuccessMessageVisible, isSivaConfirmed);
    }

    @Override
    public Action action() {
        return Action.ContainerLoadAction.create(containerFile, signatureAddMethod,
                signatureAddSuccessMessageVisible, isExistingContainer, isSivaConfirmed);
    }
}

class NameUpdateIntent implements Intent, Action {

    @Nullable File containerFile;

    @Nullable String name;

    public NameUpdateIntent(@Nullable File containerFile, @Nullable String name) {
        this.containerFile = containerFile;
        this.name = name;
    }

    static NameUpdateIntent show(File file) {
        return create(file, null);
    }

    static NameUpdateIntent update(File file, String name) {
        return create(file, name);
    }

    static NameUpdateIntent clear() {
        return create(null, null);
    }

    private static NameUpdateIntent create(@Nullable File containerFile,
                                           @Nullable String name) {
        return new NameUpdateIntent(containerFile, name);
    }

    @Override
    public Action action() {
        return null;
    }
}

class DocumentsAddIntent implements Intent {
    @Nullable private final File containerFile;

    public DocumentsAddIntent(@Nullable File containerFile) {
        this.containerFile = containerFile;
    }

    static DocumentsAddIntent create(File containerFile) {
        return new DocumentsAddIntent(containerFile);
    }

    static DocumentsAddIntent clear() {
        return new DocumentsAddIntent(null);
    }

    @Override
    public Action action() {
        return Action.DocumentsAddAction.create(containerFile);
    }
}

class DocumentViewIntent implements Intent, Action {

    @Nullable File containerFile;
    @Nullable DataFile document;
    boolean confirmation;

    public DocumentViewIntent(@Nullable File containerFile, @Nullable DataFile document, boolean confirmation) {
        this.containerFile = containerFile;
        this.document = document;
        this.confirmation = confirmation;
    }

    static DocumentViewIntent confirmation(Context context, File containerFile, DataFile document) {
        String containerFileExtension = getFileExtension(containerFile.getName()).toLowerCase(Locale.US);
        String documentFileExtension = getFileExtension(document.name()).toLowerCase(Locale.US);
        if (!containerFileExtension.equals("pdf") && SignedContainer.isContainer(context, containerFile)) {
            try {
                boolean isConfirmationNeeded = SivaUtil.isSivaConfirmationNeeded(containerFile, document);
                return create(containerFile, document, isConfirmationNeeded);
            } catch (Exception e) {
                Timber.log(Log.ERROR, e, "Unable to get data file from container");
                return create(containerFile, document, false);
            }
        } else if (containerFileExtension.equals("pdf") && documentFileExtension.equals("pdf")) {
            return create(containerFile, document, false);
        } else {
            boolean isConfirmationNeeded = SivaUtil.isSivaConfirmationNeeded(containerFile, document);
            return create(containerFile, document, isConfirmationNeeded);
        }

    }

    static DocumentViewIntent cancel() {
        return create(null, null, false);
    }

    static DocumentViewIntent open(File containerFile, DataFile document) {
        return create(containerFile, document, false);
    }

    static DocumentViewIntent create(@Nullable File containerFile, @Nullable DataFile document, boolean confirmation) {
        return new DocumentViewIntent(containerFile, document, confirmation);
    }

    @Override
    public Action action() {
        return null;
    }
}

class DocumentSaveIntent implements Intent, Action {

    File containerFile;

    DataFile document;

    boolean confirmation;

    public DocumentSaveIntent(File containerFile, DataFile document, boolean confirmation) {
        this.containerFile = containerFile;
        this.document = document;
        this.confirmation = confirmation;
    }

    static DocumentSaveIntent create(File containerFile, DataFile document, boolean confirmation) {
        return new DocumentSaveIntent(containerFile, document, confirmation);
    }

    @Override
    public Action action() {
        return null;
    }
}

class SignatureViewIntent implements Intent {

    File containerFile;

    Signature signature;

    boolean isSivaConfirmed;

    public SignatureViewIntent(File containerFile, Signature signature, boolean isSivaConfirmed) {
        this.containerFile = containerFile;
        this.signature = signature;
        this.isSivaConfirmed = isSivaConfirmed;
    }

    static SignatureViewIntent create(File containerFile, Signature signature, boolean isSivaConfirmed) {
        return new SignatureViewIntent(containerFile, signature, isSivaConfirmed);
    }

    @Override
    public Action action() {
        return Action.SignatureViewAction.create(containerFile, signature, isSivaConfirmed);
    }
}

class DocumentRemoveIntent implements Intent {

    boolean showConfirmation;
    @Nullable File containerFile;
    ImmutableList<DataFile> documents;

    @Nullable DataFile document;

    public DocumentRemoveIntent(boolean showConfirmation, File containerFile, ImmutableList<DataFile> documents, DataFile document) {
        this.showConfirmation = showConfirmation;
        this.containerFile = containerFile;
        this.documents = documents;
        this.document = document;
    }

    static DocumentRemoveIntent showConfirmation(File containerFile, ImmutableList<DataFile> documents, DataFile document) {
        return new DocumentRemoveIntent(true, containerFile, documents, document);
    }

    static DocumentRemoveIntent remove(File containerFile, ImmutableList<DataFile> documents, DataFile document) {
        return new DocumentRemoveIntent(false, containerFile, documents, document);
    }

    static DocumentRemoveIntent clear() {
        return new DocumentRemoveIntent(false, null, ImmutableList.of(), null);
    }

    @Override
    public Action action() {
        return Action.DocumentRemoveAction.create(showConfirmation, containerFile, documents, document);
    }
}

class SignatureRemoveIntent implements Intent {

    boolean showConfirmation;
    @Nullable File containerFile;
    @Nullable Signature signature;

    public SignatureRemoveIntent(boolean showConfirmation, @Nullable File containerFile, @Nullable Signature signature) {
        this.showConfirmation = showConfirmation;
        this.containerFile = containerFile;
        this.signature = signature;
    }

    static SignatureRemoveIntent showConfirmation(File containerFile, Signature signature) {
        return new SignatureRemoveIntent(true, containerFile, signature);
    }

    static SignatureRemoveIntent remove(File containerFile, Signature signature) {
        return new SignatureRemoveIntent(false, containerFile, signature);
    }

    static SignatureRemoveIntent clear() {
        return new SignatureRemoveIntent(false, null, null);
    }

    @Override
    public Action action() {
        return Action.SignatureRemoveAction.create(showConfirmation, containerFile, signature);
    }
}


class SignatureRoleViewIntent implements Intent {
    Signature signature;

    public SignatureRoleViewIntent(Signature signature) {
        this.signature = signature;
    }

    static SignatureRoleViewIntent create(Signature signature) {
        return new SignatureRoleViewIntent(signature);
    }

    @Override
    public Action action() {
        return Action.SignatureRoleDetailsAction.create(signature);
    }
}

class SignatureAddIntent implements Intent {

    @Nullable Integer method;
    @Nullable Boolean existingContainer;
    @Nullable File containerFile;
    @Nullable SignatureAddRequest request;
    boolean isCancelled;
    boolean showRoleAddingView;
    @Nullable RoleData roleData;
    boolean isSivaConfirmed;

    public SignatureAddIntent(@Nullable Integer method, @Nullable Boolean existingContainer,
                              @Nullable File containerFile, @Nullable SignatureAddRequest request,
                              boolean isCancelled, boolean showRoleAddingView, @Nullable RoleData roleData,
                              boolean isSivaConfirmed) {
        this.method = method;
        this.existingContainer = existingContainer;
        this.containerFile = containerFile;
        this.request = request;
        this.isCancelled = isCancelled;
        this.showRoleAddingView = showRoleAddingView;
        this.roleData = roleData;
        this.isSivaConfirmed = isSivaConfirmed;
    }

    static SignatureAddIntent show(int method, boolean existingContainer, File containerFile, boolean showRoleAddingView, boolean isSivaConfirmed) {
        return create(method, existingContainer, containerFile, null, false, showRoleAddingView, null, isSivaConfirmed);
    }

    static SignatureAddIntent sign(int method, boolean existingContainer, File containerFile,
                                   SignatureAddRequest request, RoleData roleData, boolean isSivaConfirmed) {
        return create(method, existingContainer, containerFile, request, false, false, roleData, isSivaConfirmed);
    }

    static SignatureAddIntent clear() {
        return create(null, null, null, null, true, false, null, false);
    }

    private static SignatureAddIntent create(@Nullable Integer method,
                                             @Nullable Boolean existingContainer,
                                             @Nullable File containerFile,
                                             @Nullable SignatureAddRequest request,
                                             boolean isCancelled,
                                             boolean showRoleAddingView,
                                             @Nullable RoleData roleData,
                                             boolean isSivaConfirmed) {
        return new SignatureAddIntent(method, existingContainer, containerFile, request, isCancelled, showRoleAddingView, roleData, isSivaConfirmed);
    }

    @Override
    public Action action() {
        return Action.SignatureAddAction.create(method, existingContainer, containerFile, request,
                isCancelled, showRoleAddingView, roleData, isSivaConfirmed);
    }
}

class SendIntent implements Intent {

    File containerFile;

    public SendIntent(File containerFile) {
        this.containerFile = containerFile;
    }

    static SendIntent create(File containerFile) {
        return new SendIntent(containerFile);
    }

    @Override
    public Action action() {
        return Action.SendAction.create(containerFile);
    }
}


class ContainerSaveIntent implements Intent {

    File containerFile;

    public ContainerSaveIntent(File containerFile) {
        this.containerFile = containerFile;
    }

    static ContainerSaveIntent create(File containerFile) {
        return new ContainerSaveIntent(containerFile);
    }

    @Override
    public Action action() {
        return Action.ContainerSaveAction.create(containerFile);
    }
}

class EncryptIntent implements Intent {

    File containerFile;

    public EncryptIntent(File containerFile) {
        this.containerFile = containerFile;
    }

    static EncryptIntent create(File containerFile) {
        return new EncryptIntent(containerFile);
    }

    @Override
    public Action action() {
        return Action.EncryptAction.create(containerFile);
    }
}
