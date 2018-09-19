package ee.ria.DigiDoc.android.auth;

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import ee.ria.DigiDoc.android.utils.mvi.MviAction;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

public interface Intent extends MviIntent, MviAction {

    @AutoValue
    abstract class InitialIntent implements Intent {

        abstract boolean visible();

        static InitialIntent show() {
            return create(true);
        }

        static InitialIntent hide() {
            return create(false);
        }

        private static InitialIntent create(boolean visible) {
            return new AutoValue_Intent_InitialIntent(visible);
        }
    }

    @AutoValue
    abstract class AuthenticationIntent implements Intent {

        abstract boolean visible();

        static AuthenticationIntent show() {
            return create(true);
        }

        static AuthenticationIntent hide() {
            return create(false);
        }

        private static AuthenticationIntent create(boolean visible) {
            return new AutoValue_Intent_AuthenticationIntent(visible);
        }
    }

    @AutoValue
    abstract class AuthActionIntent implements Intent {

        @Nullable
        abstract AuthRequest request();

        public static AuthActionIntent start(AuthRequest request) {
            return create(request);
        }

        static AuthActionIntent cancel() {
            return create(null);
        }

        private static AuthActionIntent create(@Nullable AuthRequest request) {
            return new AutoValue_Intent_AuthActionIntent(request);
        }
    }
}
