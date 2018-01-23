package ee.ria.DigiDoc.android.utils;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

public final class Immutables {

    public static <T> ImmutableSet<T> with(ImmutableSet<T> set, T value) {
        return ImmutableSet.<T>builder()
                .addAll(set)
                .add(value)
                .build();
    }

    @SuppressWarnings("Guava")
    public static <T> ImmutableSet<T> without(ImmutableSet<T> set, T value) {
        return FluentIterable.from(set)
                .filter(not(equalTo(value)))
                .toSet();
    }

    /**
     * Check that list contains particular subclass instance.
     */
    public static <T> boolean containsType(ImmutableList<T> list, Class<? extends T> type) {
        for (T item : list) {
            if (type.isInstance(item)) {
                return true;
            }
        }
        return false;
    }

    private Immutables() {}
}
