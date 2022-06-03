package ee.ria.DigiDoc.android.main.diagnostics;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;
import java.util.List;

import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static Intent.InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class DiagnosticsSaveIntent implements Intent {

        @Nullable
        abstract File diagnosticsFile();

        static DiagnosticsSaveIntent create(@Nullable File diagnosticsFile) {
            return new AutoValue_Intent_DiagnosticsSaveIntent(diagnosticsFile);
        }
    }

    @AutoValue
    abstract class DiagnosticsLogsSaveIntent implements Intent {

        @Nullable
        abstract File logFile();

        static DiagnosticsLogsSaveIntent create(@Nullable File logFile) {
            return new AutoValue_Intent_DiagnosticsLogsSaveIntent(logFile);
        }
    }
}
