package ee.ria.DigiDoc.android.signature.update;

import static com.google.common.io.Files.getFileExtension;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Locale;

import ee.ria.DigiDoc.android.utils.SivaUtil;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import timber.log.Timber;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        abstract boolean isExistingContainer();

        abstract File containerFile();

        @Nullable abstract Integer signatureAddMethod();

        abstract boolean signatureAddSuccessMessageVisible();

        static InitialIntent create(boolean isExistingContainer, File containerFile,
                                    @Nullable Integer signatureAddMethod,
                                    boolean signatureAddSuccessMessageVisible) {
            return new AutoValue_Intent_InitialIntent(isExistingContainer, containerFile,
                    signatureAddMethod, signatureAddSuccessMessageVisible);
        }
    }

    @AutoValue
    abstract class NameUpdateIntent implements Intent, Action {

        @Nullable abstract File containerFile();

        @Nullable abstract String name();

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
            return new AutoValue_Intent_NameUpdateIntent(containerFile, name);
        }
    }

    @AutoValue
    abstract class DocumentsAddIntent implements Intent {

        @Nullable abstract File containerFile();

        static DocumentsAddIntent create(File containerFile) {
            return new AutoValue_Intent_DocumentsAddIntent(containerFile);
        }

        static DocumentsAddIntent clear() {
            return new AutoValue_Intent_DocumentsAddIntent(null);
        }
    }

    @AutoValue
    abstract class DocumentViewIntent implements Intent, Action {

        @Nullable abstract File containerFile();

        @Nullable abstract DataFile document();

        abstract boolean confirmation();

        static DocumentViewIntent confirmation(Context context, File containerFile, DataFile document) throws Exception {
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
            return new AutoValue_Intent_DocumentViewIntent(containerFile, document, confirmation);
        }
    }

    @AutoValue
    abstract class DocumentSaveIntent implements Intent, Action {

        abstract File containerFile();

        abstract DataFile document();

        static DocumentSaveIntent create(File containerFile, DataFile document) {
            return new AutoValue_Intent_DocumentSaveIntent(containerFile, document);
        }
    }

    @AutoValue
    abstract class SignatureViewIntent implements Intent {

        abstract File containerFile();

        abstract Signature signature();

        static SignatureViewIntent create(File containerFile, Signature signature) {
            return new AutoValue_Intent_SignatureViewIntent(containerFile, signature);
        }
    }

    @AutoValue
    abstract class DocumentRemoveIntent implements Intent {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        abstract ImmutableList<DataFile> documents();

        @Nullable abstract DataFile document();

        static DocumentRemoveIntent showConfirmation(File containerFile, ImmutableList<DataFile> documents, DataFile document) {
            return new AutoValue_Intent_DocumentRemoveIntent(true, containerFile, documents, document);
        }

        static DocumentRemoveIntent remove(File containerFile, ImmutableList<DataFile> documents, DataFile document) {
            return new AutoValue_Intent_DocumentRemoveIntent(false, containerFile, documents, document);
        }

        static DocumentRemoveIntent clear() {
            return new AutoValue_Intent_DocumentRemoveIntent(false, null, ImmutableList.of(), null);
        }
    }

    @AutoValue
    abstract class SignatureRemoveIntent implements Intent {

        abstract boolean showConfirmation();

        @Nullable abstract File containerFile();

        @Nullable abstract Signature signature();

        static SignatureRemoveIntent showConfirmation(File containerFile, Signature signature) {
            return new AutoValue_Intent_SignatureRemoveIntent(true, containerFile, signature);
        }

        static SignatureRemoveIntent remove(File containerFile, Signature signature) {
            return new AutoValue_Intent_SignatureRemoveIntent(false, containerFile, signature);
        }

        static SignatureRemoveIntent clear() {
            return new AutoValue_Intent_SignatureRemoveIntent(false, null, null);
        }
    }

    @AutoValue
    abstract class SignatureAddIntent implements Intent {

        @Nullable abstract Integer method();

        @Nullable abstract Boolean existingContainer();

        @Nullable abstract File containerFile();

        @Nullable abstract SignatureAddRequest request();

        abstract boolean isCancelled();

        static SignatureAddIntent show(int method, boolean existingContainer, File containerFile) {
            return create(method, existingContainer, containerFile, null, false);
        }

        static SignatureAddIntent sign(int method, boolean existingContainer, File containerFile,
                                       SignatureAddRequest request) {
            return create(method, existingContainer, containerFile, request, false);
        }

        static SignatureAddIntent clear() {
            return create(null, null, null, null, true);
        }

        private static SignatureAddIntent create(@Nullable Integer method,
                                                 @Nullable Boolean existingContainer,
                                                 @Nullable File containerFile,
                                                 @Nullable SignatureAddRequest request,
                                                 boolean isCancelled) {
            return new AutoValue_Intent_SignatureAddIntent(method, existingContainer, containerFile,
                    request, isCancelled);
        }
    }

    @AutoValue
    abstract class SendIntent implements Intent {

        abstract File containerFile();

        static SendIntent create(File containerFile) {
            return new AutoValue_Intent_SendIntent(containerFile);
        }
    }
}
