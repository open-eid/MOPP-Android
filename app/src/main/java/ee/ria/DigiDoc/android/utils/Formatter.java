package ee.ria.DigiDoc.android.utils;

import android.app.Application;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.xml.sax.XMLReader;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.model.EIDType;
import ee.ria.tokenlibrary.Token;

public final class Formatter {

    private final Application application;

    @Inject Formatter(Application application) {
        this.application = application;
    }

    public void underline(TextView textView) {
        textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    public CharSequence instant(Instant instant) {
        Date date = new Date(instant.toEpochMilli());
        return String.format(Locale.US, "%s %s", dateFormat().format(date),
                timeFormat().format(date));
    }

    public CharSequence eidType(@Nullable @EIDType String eidType) {
        return application.getString(EID_TYPES.getOrDefault(eidType, R.string.eid_type_unknown));
    }

    public CharSequence idCardExpiryDate(@Nullable LocalDate expiryDate) {
        if (expiryDate == null) {
            return application.getString(R.string.eid_home_data_expiry_date_invalid);
        }
        String date = dateFormat(expiryDate.getYear(), expiryDate.getMonthValue() - 1,
                expiryDate.getDayOfMonth());
        boolean expired = LocalDate.now().isAfter(expiryDate);
        int color = ResourcesCompat.getColor(application.getResources(),
                expired ? R.color.error : R.color.success, null);
        int stringRes = expired
                ? R.string.eid_home_data_expiry_date_expired
                : R.string.eid_home_data_expiry_date_valid;
        SpannableString validityIndicator = new SpannableString(application.getString(stringRes));
        validityIndicator.setSpan(new ForegroundColorSpan(color), 0, validityIndicator.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return new SpannableStringBuilder()
                .append(date)
                .append(" | ")
                .append(validityIndicator);
    }

    public CharSequence certificateDataValidity(Token.CertType type,
                                                CertificateData certificateData) {
        ZonedDateTime notAfter = certificateData.notAfter().atZone(ZoneId.systemDefault());
        boolean expired = certificateData.expired();
        int color = ResourcesCompat.getColor(application.getResources(),
                expired ? R.color.error : R.color.success, null);
        String string;
        if (expired) {
            String pin = application.getString(type.equals(Token.CertType.CertAuth)
                    ? R.string.eid_home_certificate_pin_auth
                    : R.string.eid_home_certificate_pin_sign);
            string = application.getString(R.string.eid_home_certificate_data_expired, pin);
        } else {
            String date = dateFormat(notAfter.getYear(), notAfter.getMonthValue() - 1,
                    notAfter.getDayOfMonth());
            string = application.getString(R.string.eid_home_certificate_data_valid, date);
        }
        return fromHtml(string, new ForegroundColorTagHandler(color));
    }

    private String dateFormat(int year, int month, int day) {
        return dateFormat().format(new GregorianCalendar(year, month, day).getTime());
    }

    private DateFormat dateFormat() {
        return android.text.format.DateFormat.getMediumDateFormat(application);
    }

    private DateFormat timeFormat() {
        return android.text.format.DateFormat.getTimeFormat(application);
    }

    private static final ImmutableMap<String, Integer> EID_TYPES =
            ImmutableMap.<String, Integer>builder()
                    .put(EIDType.ID_CARD, R.string.eid_type_id_card)
                    .put(EIDType.DIGI_ID, R.string.eid_type_digi_id)
                    .put(EIDType.MOBILE_ID, R.string.eid_type_mobile_id)
                    .build();

    static final class ForegroundColorTagHandler implements Html.TagHandler {

        private final int color;

        ForegroundColorTagHandler(@ColorInt int color) {
            this.color = color;
        }

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equals("c")) {
                processStrike(opening, output);
            }
        }

        private void processStrike(boolean opening, Editable output) {
            int len = output.length();
            if (opening) {
                output.setSpan(new ForegroundColorSpan(color), len, len, Spannable.SPAN_MARK_MARK);
            } else {
                Object obj = getLast(output, ForegroundColorSpan.class);
                int where = output.getSpanStart(obj);
                output.removeSpan(obj);
                if (where != len) {
                    output.setSpan(new ForegroundColorSpan(color), where, len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private Object getLast(Editable text, Class<?> kind) {
            Object[] spans = text.getSpans(0, text.length(), kind);
            if (spans.length == 0) {
                return null;
            } else {
                for(int i = spans.length; i > 0; i--) {
                    if(text.getSpanFlags(spans[i - 1]) == Spannable.SPAN_MARK_MARK) {
                        return spans[i - 1];
                    }
                }
                return null;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String source, Html.TagHandler tagHandler) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, 0, null, tagHandler);
        } else {
            return Html.fromHtml(source, null, tagHandler);
        }
    }
}
