package ee.ria.DigiDoc.android.main.diagnostics;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviResult;
import ee.ria.DigiDoc.android.utils.mvi.State;

interface Result extends MviResult<ViewState> {

    @AutoValue
    abstract class InitialResult implements Result {

        @State abstract String state();

        @Nullable abstract Throwable error();

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static InitialResult activity() {
            return create(State.ACTIVE, null);
        }

        private static InitialResult create(@State String state, @Nullable Throwable error) {
            return new AutoValue_Result_InitialResult(state, error);
        }
    }

    @AutoValue
    abstract class DiagnosticsSaveResult implements Result {

        @State
        abstract String state();

        @Nullable abstract Throwable error();

        static DiagnosticsSaveResult activity() {
            return create(State.ACTIVE, null);
        }

        @Override
        public ViewState reduce(ViewState state) {
            return state;
        }

        static DiagnosticsSaveResult success() {
            return create(State.IDLE, null);
        }

        static DiagnosticsSaveResult failure(Throwable error) {
            return create(State.IDLE, error);
        }

        private static DiagnosticsSaveResult create(@State String state, @Nullable Throwable error) {
            return new AutoValue_Result_DiagnosticsSaveResult(state, error);
        }
    }
}
