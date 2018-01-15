package ee.ria.mopplib.data;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@AutoValue
public abstract class SignedContainer {

    public abstract ImmutableList<DataFile> dataFiles();

    public abstract ImmutableList<Signature> signatures();

    public final SignedContainer addDataFiles(ImmutableList<File> dataFiles) {
        return null;
    }

    public final SignedContainer removeDataFile(DataFile dataFile) throws
            ContainerDataFilesEmptyException {
        return null;
    }

    public final File getDataFile(DataFile dataFile, File directory) {
        return null;
    }

    public final SignedContainer addAdEsSignature(byte[] adEsSignature) throws
            SignaturesLockedException {
        return null;
    }

    public final SignedContainer removeSignature(Signature signature) throws
            SignaturesLockedException {
        return null;
    }

    /**
     * Create a new signed container with given data files.
     *
     * @param file Path to the created container.
     * @param dataFiles List of paths to data files.
     * @return New signed container with given data files and no signatures.
     * @throws IOException When given paths are inaccessible.
     * @throws ContainerDataFilesEmptyException When no data files are given.
     */
    public static SignedContainer create(File file, ImmutableList<File> dataFiles) throws
            IOException, ContainerDataFilesEmptyException {
        return null;
    }

    /**
     * Open a signed container from {@link File}.
     *
     * @param file Path to existing container.
     * @return Signed container with data files and signatures.
     * @throws FileNotFoundException When file could not be found/opened.
     */
    public static SignedContainer open(File file) throws FileNotFoundException {
        return null;
    }
}
