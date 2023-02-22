package ee.ria.DigiDoc.android.utils.files;

import java.io.File;
import java.io.IOException;

public final class FileAlreadyExistsException extends IOException {

    public final File file;
    public final String message;

    public FileAlreadyExistsException(File file) {
        this.file = file;
        this.message = null;
    }

    public FileAlreadyExistsException(String message) {
        this.file = null;
        this.message = message;
    }
}
