package ee.ria.DigiDoc.android.utils;

import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.google.common.collect.ImmutableMap;

import org.xml.sax.XMLReader;

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.EIDType;
import ee.ria.DigiDoc.idcard.CertificateType;

public final class Formatter {

    private final Navigator navigator;

    @Inject Formatter(Navigator navigator) {
        this.navigator = navigator;
    }

    public void underline(TextView textView) {
        textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    public CharSequence instant(Instant instant) {
        Date date = new Date(instant.toEpochMilli());
        return String.format(Locale.US, "%s %s", dateFormat().format(date),
                timeFormat().format(date));
    }

    public String instantAccessibility(Instant instant, boolean withTime) {
        Locale locale = Locale.forLanguageTag("et");
        DateTimeFormatter dtfPattern = DateTimeFormatter.ofPattern("dd-MMM yyyy HH:mm:ss", locale);
        if (!withTime) {
            dtfPattern = DateTimeFormatter.ofPattern("dd-MMM yyyy", locale);
        }
        String dateTimeFormat = instant.atZone(ZoneId.systemDefault()).format(dtfPattern);
        return String.format(locale, "%s", dateTimeFormat);
    }

    public CharSequence eidType(EIDType eidType) {
        return resources().getString(EID_TYPES.get(eidType));
    }

    public CharSequence idCardExpiryDate(@Nullable LocalDate expiryDate) {
        if (expiryDate == null) {
            return resources().getString(R.string.eid_home_data_expiry_date_invalid);
        }
        String date = dateFormat(expiryDate.getYear(), expiryDate.getMonthValue() - 1,
                expiryDate.getDayOfMonth());
        boolean expired = LocalDate.now().isAfter(expiryDate);
        int color = ResourcesCompat.getColor(resources(),
                expired ? R.color.error : R.color.success, null);
        int stringRes = expired
                ? R.string.eid_home_data_expiry_date_expired
                : R.string.eid_home_data_expiry_date_valid;
        SpannableString validityIndicator = new SpannableString(resources().getString(stringRes));
        validityIndicator.setSpan(new ForegroundColorSpan(color), 0, validityIndicator.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return new SpannableStringBuilder()
                .append(date)
                .append(" | ")
                .append(validityIndicator);
    }

    public CharSequence idCardExpiryDateAccessibility(@Nullable CharSequence text, @Nullable LocalDate expiryDate) {
        if (text == null || expiryDate == null) {
            return resources().getString(R.string.eid_home_data_expiry_date_invalid);
        }
        String[] idCardExpiryDateDescription = text.toString().split("\\|");
        idCardExpiryDateDescription[0] = instantAccessibility(expiryDate.atStartOfDay().toInstant(ZoneOffset.UTC), false);
        return TextUtils.join(" ", idCardExpiryDateDescription);
    }

    public CharSequence certificateDataValidity(CertificateType type, Certificate certificate) {
        ZonedDateTime notAfter = certificate.notAfter().atZone(ZoneId.systemDefault());
        boolean expired = certificate.expired();
        int color = ResourcesCompat.getColor(resources(),
                expired ? R.color.error : R.color.success, null);
        String string;
        if (expired) {
            String pin = resources().getString(type.equals(CertificateType.AUTHENTICATION)
                    ? R.string.eid_home_certificate_pin_auth
                    : R.string.eid_home_certificate_pin_sign);
            string = resources().getString(R.string.eid_home_certificate_data_expired, pin);
        } else {
            String date = dateFormat(notAfter.getYear(), notAfter.getMonthValue() - 1,
                    notAfter.getDayOfMonth());
            string = resources().getString(R.string.eid_home_certificate_data_valid, date);
        }
        return fromHtml(string, new ForegroundColorTagHandler(color));
    }

    private String dateFormat(int year, int month, int day) {
        return dateFormat().format(new GregorianCalendar(year, month, day).getTime());
    }

    private DateFormat dateFormat() {
        return android.text.format.DateFormat.getMediumDateFormat(navigator.activity());
    }

    private DateFormat timeFormat() {
        return android.text.format.DateFormat.getTimeFormat(navigator.activity());
    }

    private Resources resources() {
        return navigator.activity().getResources();
    }

    private static final ImmutableMap<EIDType, Integer> EID_TYPES =
            ImmutableMap.<EIDType, Integer>builder()
                    .put(EIDType.UNKNOWN, R.string.eid_type_unknown)
                    .put(EIDType.ID_CARD, R.string.eid_type_id_card)
                    .put(EIDType.DIGI_ID, R.string.eid_type_digi_id)
                    .put(EIDType.MOBILE_ID, R.string.eid_type_mobile_id)
                    .put(EIDType.E_SEAL, R.string.eid_type_e_seal)
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
        return Html.fromHtml(source, 0, null, tagHandler);
    }
}
