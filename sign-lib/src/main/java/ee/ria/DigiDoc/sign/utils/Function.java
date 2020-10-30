package ee.ria.DigiDoc.sign.utils;

import androidx.annotation.NonNull;

/**
 * Like {@link com.google.common.base.Function} but throws Exception.
 */
public interface Function<T, R> {

    R apply(@NonNull T t) throws Exception;
}
