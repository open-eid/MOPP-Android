package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.EIDData;
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

        @Nullable abstract EIDData data();

        @Nullable abstract Token token();

        static CodeUpdateIntent show(CodeUpdateAction action) {
            return create(action, null, null, null);
        }

        static CodeUpdateIntent request(CodeUpdateAction action, CodeUpdateRequest request,
                                        EIDData data, Token token) {
            return create(action, request, data, token);
        }

        static CodeUpdateIntent clear() {
            return create(null, null, null, null);
        }

        static CodeUpdateIntent create(@Nullable CodeUpdateAction action,
                                       @Nullable CodeUpdateRequest request,
                                       @Nullable EIDData data, @Nullable Token token) {
            return new AutoValue_Intent_CodeUpdateIntent(action, request, data, token);
        }
    }
}
