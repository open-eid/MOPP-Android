package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;

interface Action extends MviAction {

    @AutoValue
    abstract class ContainerLoadAction implements Action {

        abstract File containerFile();

        static ContainerLoadAction create(File containerFile) {
            return new AutoValue_Action_ContainerLoadAction(containerFile);
        }
    }

    @AutoValue
    abstract class DocumentsAddAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract ImmutableList<FileStream> fileStreams();

        static DocumentsAddAction create(@Nullable File containerFile,
                                         @Nullable ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Action_DocumentsAddAction(containerFile, fileStreams);
        }
    }

    @AutoValue
    abstract class DocumentOpenAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract DataFile document();

        static DocumentOpenAction create(@Nullable File containerFile,
                                         @Nullable DataFile document) {
            return new AutoValue_Action_DocumentOpenAction(containerFile, document);
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

        abstract boolean show();

        @Nullable abstract File containerFile();

        @Nullable abstract String phoneNo();

        @Nullable abstract String personalCode();

        @Nullable abstract Boolean rememberMe();

        static SignatureAddAction create(boolean show, @Nullable File containerFile,
                                         @Nullable String phoneNo, @Nullable String personalCode,
                                         @Nullable Boolean rememberMe) {
            return new AutoValue_Action_SignatureAddAction(show, containerFile, phoneNo,
                    personalCode, rememberMe);
        }
    }
}
