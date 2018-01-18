package ee.ria.mopplib.data;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DataFile {

    /**
     * Unique ID per container.
     */
    public abstract String id();

    /**
     * Filename.
     */
    public abstract String name();

    /**
     * File size in bytes.
     */
    public abstract long size();

    /**
     * Creates a new data file object.
     *
     * Should only be accessed from {@link SignedContainer}.
     *
     * @param id Data file ID.
     * @param name Data file name.
     * @param size Data file size in bytes.
     * @return Data file instance.
     */
    static DataFile create(String id, String name, long size) {
        return new AutoValue_DataFile(id, name, size);
    }
}
