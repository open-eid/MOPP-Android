package ee.ria.DigiDoc.android.utils;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.files.FileStream;

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
                builder.add(FileStream.create(contentResolver, uri));
            }
        } else if (data != null) {
            builder.add(FileStream.create(contentResolver, data));
        }

        return builder.build();
    }

    /**
     * Create an intent to send local file to other apps.
     *
     * File path has to be shared with {@link FileProvider}.
     *
     * @param context Context to use for {@link FileProvider#getUriForFile(Context, String, File)}
     *                and to get authority string.
     * @param file File to send.
     * @return {@link Intent#ACTION_SEND Send intent} with content Uri of the file.
     */
    public static Intent createViewIntent(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getString(R.string.file_provider_authority), file);
        return Intent
                .createChooser(new Intent(Intent.ACTION_VIEW)
                        .setData(uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null);
    }

    private IntentUtils() {}
}
