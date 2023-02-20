package ee.ria.DigiDoc.android.utils;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

@SuppressWarnings("Guava")
public final class Immutables {

    public static <T> ImmutableSet<T> with(ImmutableSet<T> set, T value) {
        return ImmutableSet.<T>builder()
                .addAll(set)
                .add(value)
                .build();
    }

    public static <T> ImmutableSet<T> without(ImmutableSet<T> set, T value) {
        return FluentIterable.from(set)
                .filter(not(equalTo(value)))
                .toSet();
    }

    public static <T> ImmutableList<T> with(ImmutableList<T> list, T value, boolean append) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        if (append) {
            builder.addAll(list).add(value);
        } else {
            builder.add(value).addAll(list);
        }
        return builder.build();
    }

    public static <T> ImmutableList<T> merge(ImmutableList<T> list, ImmutableList<T> values) {
        ImmutableSet.Builder<T> builder = ImmutableSet.builder();
        builder.addAll(values).addAll(list);
        return builder.build().asList();
    }

    public static <T> ImmutableList<T> without(ImmutableList<T> list, T value) {
        return FluentIterable.from(list)
                .filter(not(equalTo(value)))
                .toList();
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
