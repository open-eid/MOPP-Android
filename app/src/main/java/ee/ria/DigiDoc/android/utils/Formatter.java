package ee.ria.DigiDoc.android.utils;

import android.app.Application;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.EIDType;
import ee.ria.mopplib.data.DataFile;

import static com.google.common.io.Files.getFileExtension;

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

    public CharSequence fileSize(long fileSize) {
        return android.text.format.Formatter.formatShortFileSize(application, fileSize);
    }

    public CharSequence eidType(@Nullable @EIDType String eidType) {
        return application.getString(EID_TYPES.getOrDefault(eidType, R.string.eid_type_unknown));
    }

    public CharSequence idCardExpiryDate(@Nullable LocalDate expiryDate) {
        if (expiryDate == null) {
            return application.getString(R.string.eid_home_data_expiry_date_invalid);
        }
        String date = dateFormat().format(new GregorianCalendar(expiryDate.getYear(),
                expiryDate.getMonthValue() - 1, expiryDate.getDayOfMonth()).getTime());
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

    @DrawableRes public int documentTypeImageRes(DataFile document) {
        String extension = getFileExtension(document.name()).toLowerCase(Locale.US);
        for (Map.Entry<Integer, String> entry : DOCUMENT_TYPE_MAP.entries()) {
            if (entry.getValue().equals(extension)) {
                return entry.getKey();
            }
        }
        return DEFAULT_FILE_TYPE;
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

    @DrawableRes private static final int DEFAULT_FILE_TYPE = R.drawable.ic_insert_drive_file;

    private static final ImmutableSetMultimap<Integer, String> DOCUMENT_TYPE_MAP =
            ImmutableSetMultimap.<Integer, String>builder()
                    .putAll(R.drawable.ic_file_word, "doc", "dot", "wbk", "docx", "docm", "dotx",
                            "dotm", "docb", "odt", "ott", "oth", "odm", "sxw", "stw", "sxg")
                    .putAll(R.drawable.ic_file_excel, "xls", "xlt", "xlt", "xlsx", "xlsm", "xltx",
                            "xltm", "xlsb", "xla", "xlam", "xll", "xlw", "ods", "ots", "sxc", "stc")
                    .putAll(R.drawable.ic_file_powerpoint, "ppt", "pot", "pps", "pptx", "pptm",
                            "potx", "potm", "ppam", "ppsx", "ppsm", "sldx", "sldm", "odp", "odg",
                            "otp", "sxi", "sti")
                    .putAll(R.drawable.ic_file_image, "tif", "tiff", "bmp", "jpg", "jpeg", "gif",
                            "png", "eps", "raw", "cr2", "nef", "orf", "sr2")
                    .putAll(R.drawable.ic_file_music, "pcm", "wav", "aiff", "mp3", "aac", "ogg",
                            "wma", "flac", "alac")
                    .putAll(R.drawable.ic_file_video, "avi", "asf", "mov", "qt", "mpg", "mpeg",
                            "wmv", "rm", "mkv")
                    .putAll(R.drawable.ic_file_pdf, "pdf")
                    .putAll(R.drawable.ic_file_xml, "xml", "html", "java", "c", "cpp", "php")
                    .build();
}
