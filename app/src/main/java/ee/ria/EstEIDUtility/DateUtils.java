package ee.ria.EstEIDUtility;


import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static boolean isToday(Date date1) {
        if (date1 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        return isSameDay(cal1);
    }

    public static boolean isSameDay(Calendar cal1) {
        Date today = Calendar.getInstance().getTime();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(today);
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    public static boolean isYesterday(Date date) {
        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.DAY_OF_YEAR, -1);

        Calendar c2 = Calendar.getInstance();
        c2.setTime(date);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isCurrentYear(Date date) {
        int year = Calendar.getInstance().get(Calendar.YEAR);

        Calendar c1 = Calendar.getInstance();
        c1.setTime(date);

        return year == c1.get(Calendar.YEAR);
    }
}
