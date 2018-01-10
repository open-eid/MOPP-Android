package ee.ria.DigiDoc.android.utils;

import android.app.Application;
import android.support.annotation.DrawableRes;

import com.google.common.collect.ImmutableSetMultimap;

import org.threeten.bp.Instant;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.document.data.Document;

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

    @DrawableRes public int documentTypeImageRes(Document document) {
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
