package ee.ria.DigiDoc.android.utils.files;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.auto.value.AutoValue;
import com.google.common.io.ByteSource;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import timber.log.Timber;

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
        String cleanUri = FilenameUtils.getName(uri.getScheme()) + ":" + cleanString(uri.getSchemeSpecificPart());
        String cleanUri2 = sanitizeString(uri.toString(), '_');
        String cleanUri3 = FilenameUtils.getName(uri.getScheme()) + ":" + sanitizeString(uri.getSchemeSpecificPart(), '_');
        String fullPath = FilenameUtils.getName(uri.getScheme()) + ":" + FilenameUtils.getFullPath(uri.getSchemeSpecificPart()) + Uri.encode(FilenameUtils.getName(uri.getLastPathSegment()));
        Cursor cursor = contentResolver.query(Uri.parse(fullPath), new String[]{OpenableColumns.DISPLAY_NAME}, null,
                null, null);
        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                displayName = cursor.getString(0);
            }
            cursor.close();
        }


        Cursor cursor2 = contentResolver.query(Uri.parse(cleanUri), new String[]{OpenableColumns.DISPLAY_NAME}, null,
                null, null);
        if (cursor2 != null) {
            if (cursor2.moveToFirst() && !cursor2.isNull(0)) {
                displayName = cursor2.getString(0);
            }
            cursor2.close();
        }

        FileStream fileStream1 = new AutoValue_FileStream(displayName,
                new ContentResolverUriSource(contentResolver, Uri.parse(cleanUri)));

        Timber.d(fileStream1.displayName());


        Cursor cursor3 = contentResolver.query(Uri.parse(cleanUri2), new String[]{OpenableColumns.DISPLAY_NAME}, null,
                null, null);
        if (cursor3 != null) {
            if (cursor3.moveToFirst() && !cursor3.isNull(0)) {
                displayName = cursor3.getString(0);
            }
            cursor3.close();
        }

        FileStream fileStream2 = new AutoValue_FileStream(displayName,
                new ContentResolverUriSource(contentResolver, Uri.parse(cleanUri2)));

        Timber.d(fileStream2.displayName());

        Cursor cursor4 = contentResolver.query(Uri.parse(cleanUri3), new String[]{OpenableColumns.DISPLAY_NAME}, null,
                null, null);
        if (cursor4 != null) {
            if (cursor4.moveToFirst() && !cursor4.isNull(0)) {
                displayName = cursor4.getString(0);
            }
            cursor4.close();
        }

        FileStream fileStream3 = new AutoValue_FileStream(displayName,
                new ContentResolverUriSource(contentResolver, Uri.parse(cleanUri3)));

        Timber.d(fileStream3.displayName());


        return new AutoValue_FileStream(displayName,
                new ContentResolverUriSource(contentResolver, Uri.parse(fullPath)));
    }

    /**
     * Create FileStream from {@link File}.
     */
    public static FileStream create(File file) {
        return new AutoValue_FileStream(file.getName(), asByteSource(file));
    }

    public static String sanitizeString(String input, char replacement) {
        String allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-,.:/%";

        StringBuilder sb = new StringBuilder();

        for (int offset = 0; offset < input.length(); offset++) {
            char c = input.charAt(offset);

            if (allowedCharacters.indexOf(c) == -1) {
                sb.append(replacement);
            }
            else {
                sb.append(allowedCharacters.charAt(allowedCharacters.indexOf(c)));
            }
        }

        return sb.toString();
    }

    public static String cleanString(String aString) {
        if (aString == null) return null;
        String cleanString = "";
        for (int i = 0; i < aString.length(); ++i) {
            cleanString += cleanChar(aString.charAt(i));
        }
        return cleanString;
    }

    private static char cleanChar(char aChar) {

        // 0 - 9
        for (int i = 48; i < 58; ++i) {
            if (aChar == i) return (char) i;
        }

        // 'A' - 'Z'
        for (int i = 65; i < 91; ++i) {
            if (aChar == i) return (char) i;
        }

        // 'a' - 'z'
        for (int i = 97; i < 123; ++i) {
            if (aChar == i) return (char) i;
        }

        // other valid characters
        switch (aChar) {
            case '/':
                return '/';
            case '.':
                return '.';
            case '-':
                return '-';
            case '_':
                return '_';
            case ' ':
                return ' ';
        }
        return '%';
    }

    }
