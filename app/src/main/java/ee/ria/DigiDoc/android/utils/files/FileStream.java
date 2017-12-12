package ee.ria.DigiDoc.android.utils.files;

import com.google.auto.value.AutoValue;

import java.io.InputStream;

@AutoValue
public abstract class FileStream {

    public abstract String displayName();

    public abstract InputStream inputStream();

    public static FileStream create(String displayName, InputStream inputStream) {
        return new AutoValue_FileStream(displayName, inputStream);
    }
}
