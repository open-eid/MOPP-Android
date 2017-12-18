package ee.ria.DigiDoc.android.utils.files;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.auto.value.AutoValue;
import com.google.common.io.ByteSource;

@AutoValue
public abstract class FileStream {

    public abstract String displayName();

    public abstract ByteSource source();

    /**
     * Create FileStream instance from {@link ContentResolver} and content {@link Uri}.
     */
    public static FileStream create(ContentResolver contentResolver, Uri uri) {
        String displayName = uri.getLastPathSegment();
        Cursor cursor = contentResolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null,
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
}
