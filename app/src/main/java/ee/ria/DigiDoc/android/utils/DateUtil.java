package ee.ria.DigiDoc.android.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

public final class DateUtil {
    public static Instant toEpochSecond(int year, Month month, int day, int hour, int minute, int second) {
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
        return Instant.ofEpochSecond(localDateTime.toEpochSecond(ZoneOffset.UTC));
    }
}
