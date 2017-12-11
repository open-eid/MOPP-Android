package ee.ria.DigiDoc.android.utils;

import android.app.Application;

import org.threeten.bp.Instant;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

public final class Formatter {

    private final Application application;

    @Inject Formatter(Application application) {
        this.application = application;
    }

    public CharSequence instant(Instant instant) {
        Date date = new Date(instant.toEpochMilli());
        return String.format(Locale.US, "%s %s", dateFormat().format(date),
                timeFormat().format(date));
    }

    private DateFormat dateFormat() {
        return android.text.format.DateFormat.getMediumDateFormat(application);
    }

    private DateFormat timeFormat() {
        return android.text.format.DateFormat.getTimeFormat(application);
    }
}
