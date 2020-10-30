package ee.ria.DigiDoc.android.eid;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import ee.ria.DigiDoc.idcard.Token;

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

        @Nullable abstract CodeUpdateRequest request();

        @Nullable abstract IdCardData data();

        @Nullable abstract Token token();

        abstract boolean cleared();

        static CodeUpdateIntent show(CodeUpdateAction action) {
            return create(action, null, null, null, false);
        }

        static CodeUpdateIntent request(CodeUpdateAction action, CodeUpdateRequest request,
                                        IdCardData data, Token token) {
            return create(action, request, data, token, false);
        }

        static CodeUpdateIntent clear() {
            return create(null, null, null, null, false);
        }

        static CodeUpdateIntent clear(CodeUpdateAction action) {
            return create(action, null, null, null, true);
        }

        static CodeUpdateIntent create(@Nullable CodeUpdateAction action,
                                       @Nullable CodeUpdateRequest request,
                                       @Nullable IdCardData data, @Nullable Token token, boolean cleared) {
            return new AutoValue_Intent_CodeUpdateIntent(action, request, data, token, cleared);
        }
    }
}
