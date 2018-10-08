package ee.ria.DigiDoc.android.utils;

import android.support.annotation.Nullable;

import io.reactivex.functions.Predicate;

public final class Predicates {

    private Predicates() {}

    public static <T> Predicate<T> duplicates() {
        return new DuplicatesPredicate<>();
    }

    static final class DuplicatesPredicate<T> implements Predicate<T> {

        @Nullable private T previous;

        @Override
        public boolean test(T t) {
            if (previous != null && previous.equals(t)) {
                return false;
            } else {
                previous = t;
                return true;
            }
        }
    }
}
