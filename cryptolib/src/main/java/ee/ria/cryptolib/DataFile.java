package ee.ria.cryptolib;

import com.google.auto.value.AutoValue;

import java.io.File;

@AutoValue
public abstract class DataFile {

    public abstract File file();

    public abstract String name();

    public static DataFile create(File file) {
        return new AutoValue_DataFile(file, file.getName());
    }
}
