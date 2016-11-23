package ee.ria.EstEIDUtility.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.Signatures;

public class ContainerUtils {

    public static boolean hasDataFile(DataFiles dataFiles, String attachedName) {
        for (int i = 0; i < dataFiles.size(); i++) {
            DataFile dataFile = dataFiles.get(i);
            if (dataFile.fileName().equals(attachedName)) {
                return true;
            }
        }
        return false;
    }

    public static DataFile getDataFile(DataFiles dataFiles, String attachedName) {
        for (int i = 0; i < dataFiles.size(); i++) {
            DataFile dataFile = dataFiles.get(i);
            if (dataFile.fileName().equals(attachedName)) {
                return dataFile;
            }
        }
        return null;
    }

    public static List<DataFile> extractDataFiles(Container container) {
        if (container == null) {
            return Collections.emptyList();
        }
        DataFiles containerDataFiles = container.dataFiles();
        List<DataFile> dataFiles = new ArrayList<>();
        for (int i = 0; i < containerDataFiles.size(); i++) {
            dataFiles.add(containerDataFiles.get(i));
        }
        return dataFiles;
    }

    public static List<Signature> extractSignatures(Container container) {
        if (container == null) {
            return Collections.emptyList();
        }
        Signatures signatures = container.signatures();
        List<Signature> signatureItems = new ArrayList<>();
        for (int i = 0; i < signatures.size(); i++) {
            signatureItems.add(signatures.get(i));
        }
        return signatureItems;
    }
}
