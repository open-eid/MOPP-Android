package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;

interface Action extends MviAction {

    @AutoValue
    abstract class LoadContainerAction implements Action {

        abstract File containerFile();

        static LoadContainerAction create(File containerFile) {
            return new AutoValue_Action_LoadContainerAction(containerFile);
        }
    }

    @AutoValue
    abstract class AddDocumentsAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract ImmutableList<FileStream> fileStreams();

        static AddDocumentsAction create(@Nullable File containerFile,
                                         @Nullable ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Action_AddDocumentsAction(containerFile, fileStreams);
        }
    }

    @AutoValue
    abstract class OpenDocumentAction implements Action {

        @Nullable abstract File containerFile();

        @Nullable abstract Document document();

        static OpenDocumentAction create(@Nullable File containerFile,
                                         @Nullable Document document) {
            return new AutoValue_Action_OpenDocumentAction(containerFile, document);
        }
    }
}
