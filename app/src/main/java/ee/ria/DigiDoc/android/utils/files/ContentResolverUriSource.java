package ee.ria.DigiDoc.android.utils.files;

import android.content.ContentResolver;
import android.net.Uri;

import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;

final class ContentResolverUriSource extends ByteSource {

    private final ContentResolver contentResolver;
    private final Uri uri;

    ContentResolverUriSource(ContentResolver contentResolver, Uri uri) {
        this.contentResolver = contentResolver;
        this.uri = uri;
    }

    @Override
    public InputStream openStream() throws IOException {
        return contentResolver.openInputStream(uri);
    }
}
