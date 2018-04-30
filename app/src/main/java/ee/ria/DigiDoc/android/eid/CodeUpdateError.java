package ee.ria.DigiDoc.android.eid;

import com.google.auto.value.AutoValue;

abstract class CodeUpdateError extends Exception {

    @AutoValue
    abstract static class CodeMinLengthError extends CodeUpdateError {

        abstract int minLength();

        static CodeMinLengthError create(int minLength) {
            return new AutoValue_CodeUpdateError_CodeMinLengthError(minLength);
        }
    }

    @AutoValue
    abstract static class CodeRepeatMismatchError extends CodeUpdateError {

        static CodeRepeatMismatchError create() {
            return new AutoValue_CodeUpdateError_CodeRepeatMismatchError();
        }
    }

    @AutoValue
    abstract static class CodePartOfPersonalCodeError extends CodeUpdateError {

        static CodePartOfPersonalCodeError create() {
            return new AutoValue_CodeUpdateError_CodePartOfPersonalCodeError();
        }
    }

    @AutoValue
    abstract static class CodePartOfDateOfBirthError extends CodeUpdateError {

        static CodePartOfDateOfBirthError create() {
            return new AutoValue_CodeUpdateError_CodePartOfDateOfBirthError();
        }
    }

    @AutoValue
    abstract static class CodeTooEasyError extends CodeUpdateError {

        static CodeTooEasyError create() {
            return new AutoValue_CodeUpdateError_CodeTooEasyError();
        }
    }

    @AutoValue
    abstract static class CodeSameAsCurrentError extends CodeUpdateError {

        static CodeSameAsCurrentError create() {
            return new AutoValue_CodeUpdateError_CodeSameAsCurrentError();
        }
    }

    @AutoValue
    abstract static class CodeInvalidError extends CodeUpdateError {

        abstract int retryCount();

        static CodeInvalidError create(int retryCount) {
            return new AutoValue_CodeUpdateError_CodeInvalidError(retryCount);
        }
    }
}
