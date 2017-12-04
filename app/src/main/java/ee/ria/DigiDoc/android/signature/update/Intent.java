package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        abstract File containerFile();

        static InitialIntent create(File containerFile) {
            return new AutoValue_Intent_InitialIntent(containerFile);
        }
    }

    @AutoValue
    abstract class AddDocumentsIntent implements Intent {

        @Nullable abstract File containerFile();

        @Nullable abstract ImmutableList<FileStream> fileStreams();

        static AddDocumentsIntent pick(File containerFile) {
            return new AutoValue_Intent_AddDocumentsIntent(containerFile, null);
        }

        static AddDocumentsIntent add(File containerFile, ImmutableList<FileStream> fileStreams) {
            return new AutoValue_Intent_AddDocumentsIntent(containerFile, fileStreams);
        }

        static AddDocumentsIntent clear() {
            return new AutoValue_Intent_AddDocumentsIntent(null, null);
        }
    }
}
