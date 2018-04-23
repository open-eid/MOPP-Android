package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        static InitialIntent create() {
            return new AutoValue_Intent_InitialIntent();
        }
    }

    @AutoValue
    abstract class LoadIntent implements Intent {

        static LoadIntent create() {
            return new AutoValue_Intent_LoadIntent();
        }
    }

    @AutoValue
    abstract class CertificatesTitleClickIntent implements Intent {

        abstract boolean expand();

        static CertificatesTitleClickIntent create(boolean expand) {
            return new AutoValue_Intent_CertificatesTitleClickIntent(expand);
        }
    }

    @AutoValue
    abstract class CodeUpdateIntent implements Intent, Action {

        @Nullable abstract CodeUpdateAction action();

        static CodeUpdateIntent show(CodeUpdateAction action) {
            return create(action);
        }

        static CodeUpdateIntent clear() {
            return create(null);
        }

        static CodeUpdateIntent create(@Nullable CodeUpdateAction action) {
            return new AutoValue_Intent_CodeUpdateIntent(action);
        }
    }
}
