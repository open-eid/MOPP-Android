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
     * Mime-type for this data file.
     */
    public abstract String mimeType();

    /**
     * Creates a new data file object.
     *
     * @param id Data file ID.
     * @param name Data file name.
     * @param size Data file size in bytes.
     * @param mimeType Mime-type for this data file.
     * @return Data file instance.
     */
    public static DataFile create(String id, String name, long size, String mimeType) {
        return new AutoValue_DataFile(id, name, size, mimeType == null ? "" : mimeType);
    }
}
