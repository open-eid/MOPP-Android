package ee.ria.DigiDoc.android.utils;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.common.collect.ImmutableList;

import java.io.FileNotFoundException;
import java.io.InputStream;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import timber.log.Timber;

public final class IntentUtils {

    /**
     * Create an intent to choose multiple files of any type.
     *
     * @return Intent to use with {@link android.app.Activity#startActivityForResult(Intent, int)}.
     */
    public static Intent createGetContentIntent() {
        return Intent
                .createChooser(new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), null);
    }

    /**
     * Parse intent returned from {@link #createGetContentIntent() get content intent} to
     * {@link FileStream} objects.
     *
     * Always returns a list, even if only one file was chosen.
     *
     * @param contentResolver Content resolver to get display name, type and input stream.
     * @param intent Intent returned from
     *               {@link android.app.Activity#onActivityResult(int, int, Intent)}.
     * @return List of {@link FileStream file stream} objects.
     */
    public static ImmutableList<FileStream> parseGetContentIntent(ContentResolver contentResolver,
                                                                  Intent intent) {
        ImmutableList.Builder<FileStream> builder = ImmutableList.builder();

        ClipData clipData = intent.getClipData();
        Uri data = intent.getData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                try {
                    builder.add(parseUri(contentResolver, uri));
                } catch (FileNotFoundException e) {
                    Timber.e(e, "Could not parse Uri %s", uri);
                }
            }
        } else if (data != null) {
            try {
                builder.add(parseUri(contentResolver, data));
            } catch (FileNotFoundException e) {
                Timber.e(e, "Could not parse Uri %s", data);
            }
        }

        return builder.build();
    }

    /**
     * Parse Uri to get {@link FileStream} value.
     *
     * TODO file Uri support
     *
     * @param contentResolver Used to get all the data.
     * @param uri Uri to parse.
     * @return FileStream with all fields filled.
     * @throws FileNotFoundException If the input stream opening fails.
     */
    private static FileStream parseUri(ContentResolver contentResolver, Uri uri)
            throws FileNotFoundException {
        String type = contentResolver.getType(uri);
        InputStream inputStream = contentResolver.openInputStream(uri);
        String displayName = uri.getLastPathSegment();
        Cursor cursor = contentResolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null,
                null, null);
        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                displayName = cursor.getString(0);
            }
            cursor.close();
        }

        return FileStream.create(type, displayName, inputStream);
    }

    private IntentUtils() {}
}
