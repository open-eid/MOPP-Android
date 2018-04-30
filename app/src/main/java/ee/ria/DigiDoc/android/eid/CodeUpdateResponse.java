package ee.ria.DigiDoc.android.eid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class CodeUpdateResponse {

    boolean success() {
        return currentError() == null && newError() == null && repeatError() == null
                && error() == null;
    }

    @Nullable abstract CodeUpdateError currentError();

    @Nullable abstract CodeUpdateError newError();

    @Nullable abstract CodeUpdateError repeatError();

    @Nullable abstract Throwable error();

    abstract Builder buildWith();

    static CodeUpdateResponse valid() {
        return new AutoValue_CodeUpdateResponse.Builder().build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder currentError(@Nullable CodeUpdateError currentError);
        abstract Builder newError(@Nullable CodeUpdateError newError);
        abstract Builder repeatError(@Nullable CodeUpdateError repeatError);
        abstract Builder error(@Nullable Throwable error);
        abstract CodeUpdateResponse build();
    }
}
