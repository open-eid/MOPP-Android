package ee.ria.DigiDoc.android.utils.files;

import java.io.File;
import java.io.IOException;

public final class FileAlreadyExistsException extends IOException {

    public final File file;

    public FileAlreadyExistsException(File file) {
        this.file = file;
    }
}
