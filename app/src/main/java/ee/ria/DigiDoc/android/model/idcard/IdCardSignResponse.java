package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.mopplib.data.SignedContainer;
import ee.ria.tokenlibrary.Token;

@AutoValue
public abstract class IdCardSignResponse {

    @State public abstract String state();

    @Nullable public abstract SignedContainer container();

    @Nullable public abstract Throwable error();

    @Nullable public abstract IdCardData data();

    @Nullable public abstract Token token();

    public static IdCardSignResponse activity() {
        return create(State.ACTIVE, null, null, null, null);
    }

    public static IdCardSignResponse success(SignedContainer container) {
        return create(State.IDLE, container, null, null, null);
    }

    public static IdCardSignResponse clear(Throwable error, IdCardData data, Token token) {
        return create(State.CLEAR, null, error, data, token);
    }

    public static IdCardSignResponse failure(Throwable error, IdCardData data, Token token) {
        return create(State.IDLE, null, error, data, token);
    }

    private static IdCardSignResponse create(@State String state,
                                             @Nullable SignedContainer container,
                                             @Nullable Throwable error, @Nullable IdCardData data,
                                             @Nullable Token token) {
        return new AutoValue_IdCardSignResponse(state, container, error, data, token);
    }
}
