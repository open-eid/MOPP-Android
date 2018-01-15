package ee.ria.mopplib;

import android.support.annotation.RawRes;
import android.support.test.InstrumentationRegistry;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Files {

    public static File copyRaw(File file, @RawRes int rawRes) throws IOException {
        try (
                InputStream inputStream = InstrumentationRegistry.getContext().getResources()
                        .openRawResource(rawRes);
                OutputStream outputStream = new FileOutputStream(file)
        ) {
            ByteStreams.copy(inputStream, outputStream);
            return file;
        }
    }

    private Files() {}
}
