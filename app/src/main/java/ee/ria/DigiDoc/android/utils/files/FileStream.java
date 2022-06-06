package ee.ria.DigiDoc.android.utils.files;

import static com.google.common.io.Files.asByteSource;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.auto.value.AutoValue;
import com.google.common.io.ByteSource;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.DigiDoc.common.FileUtil;

@AutoValue
public abstract class FileStream {

    public abstract String displayName();

    public abstract ByteSource source();

    public abstract long fileSize();

    /**
     * Create FileStream from {@link ContentResolver} and content {@link Uri}.
     */
    public static FileStream create(ContentResolver contentResolver, Uri uri, long fileSize) throws Exception {
        String displayName = uri.getLastPathSegment() == null ? "newFile" : FilenameUtils.getName(uri.getLastPathSegment());
        Uri sanitizedUri = FileUtil.normalizeUri(uri);
        Cursor cursor = contentResolver.query(sanitizedUri, new String[]{OpenableColumns.DISPLAY_NAME}, null,
                null, null);
        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                displayName = FilenameUtils.getName(FileUtil.sanitizeString(cursor.getString(0), ""));
            }
            cursor.close();
        }

        return new AutoValue_FileStream(displayName,
                new ContentResolverUriSource(contentResolver, sanitizedUri), fileSize);
    }

    /**
     * Create FileStream from {@link File}.
     */
    public static FileStream create(File file) {
        return new AutoValue_FileStream(file.getName(), asByteSource(file), file.length());
    }
}
