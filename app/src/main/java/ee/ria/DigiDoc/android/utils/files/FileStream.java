package ee.ria.DigiDoc.android.utils.files;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.URLUtil;

import com.google.auto.value.AutoValue;
import com.google.common.io.ByteSource;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.io.Files.asByteSource;

@AutoValue
public abstract class FileStream {

    public abstract String displayName();

    public abstract ByteSource source();

    /**
     * Create FileStream from {@link ContentResolver} and content {@link Uri}.
     */
    public static FileStream create(ContentResolver contentResolver, Uri uri) {
        String displayName = FilenameUtils.getName(uri.getLastPathSegment());
        Uri newUri = Uri.fromParts(uri.getScheme(), uri.getSchemeSpecificPart(), uri.getFragment());
        if (newUri != null && URLUtil.isValidUrl(uri.toString()) && ((URLUtil.isContentUrl(uri.toString()) || URLUtil.isFileUrl(uri.toString()) || URLUtil.isHttpUrl(uri.toString()) || URLUtil.isHttpsUrl(uri.toString())))) {
            Cursor cursor = contentResolver.query(Uri.parse(Uri.decode(newUri.toString())).normalizeScheme(), new String[]{OpenableColumns.DISPLAY_NAME}, null,
                    null, null);
            if (cursor != null) {
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    displayName = cursor.getString(0);
                }
                cursor.close();
            }
            return new AutoValue_FileStream(displayName,
                    new ContentResolverUriSource(contentResolver, uri));
        }

        throw new IllegalStateException("Invalid URL provided");
    }

    /**
     * Create FileStream from {@link File}.
     */
    public static FileStream create(File file) {
        return new AutoValue_FileStream(file.getName(), asByteSource(file));
    }
}
