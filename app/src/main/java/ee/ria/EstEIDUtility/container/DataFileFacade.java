package ee.ria.EstEIDUtility.container;


import java.io.File;

import ee.ria.libdigidocpp.DataFile;

public class DataFileFacade {

    private DataFile containerDataFile;
    private File cachedDataFile;
    private String name;

    public DataFileFacade(DataFile containerDataFile, File cachedDataFile) {
        this.containerDataFile = containerDataFile;
        this.cachedDataFile = cachedDataFile;
    }

    public DataFile getContainerDataFile() {
        return containerDataFile;
    }

    public File getLocation() {
        return cachedDataFile;
    }

    public String getName() {
        return containerDataFile.fileName();
    }
}
