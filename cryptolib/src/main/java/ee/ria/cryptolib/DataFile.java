package ee.ria.cryptolib;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DataFile {

    public abstract String name();

    public static DataFile create(String name) {
        return new AutoValue_DataFile(name);
    }
}
