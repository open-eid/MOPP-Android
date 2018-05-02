/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.container;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signatures;

public class ContainerFacade {

    private Container container;
    private File containerFile;

    public ContainerFacade(Container container, File containerFile) {
        this.container = container;
        this.containerFile = containerFile;
    }

    public File getContainerFile() {
        return containerFile;
    }

    public DataFileFacade getDataFile(String filename) {
        DataFile dataFile;
        if ((dataFile = getContainerDataFile(filename)) != null) {
            return new DataFileFacade(dataFile);
        }
        return null;
    }

    public List<DataFileFacade> getDataFiles() {
        if (container == null) {
            return Collections.emptyList();
        }
        List<DataFileFacade> dataFiles = new ArrayList<>();
        DataFiles containerDataFiles = container.dataFiles();
        for (int i = 0; i < containerDataFiles.size(); i++) {
            dataFiles.add(getDataFile(containerDataFiles.get(i).fileName()));
        }
        return dataFiles;
    }

    public List<SignatureFacade> getSignatures() {
        if (container == null) {
            return Collections.emptyList();
        }
        Signatures signatures = container.signatures();
        List<SignatureFacade> signatureItems = new ArrayList<>();
        for (int i = 0; i < signatures.size(); i++) {
            signatureItems.add(new SignatureFacade(signatures.get(i)));
        }
        return signatureItems;
    }

    public String getName() {
        return containerFile.getName();
    }

    public long fileSize() {
        return containerFile.length();
    }

    private DataFile getContainerDataFile(String filename) {
        DataFiles containerDataFiles = container.dataFiles();
        DataFile dataFile = null;
        for (int i = 0; i < containerDataFiles.size(); i++) {
            dataFile = containerDataFiles.get(i);
            if (dataFile.fileName() != null && dataFile.fileName().equals(filename)) {
                break;
            }
        }
        return dataFile;
    }

    public String getNextSignatureId() {
        List<SignatureFacade> signatures = getSignatures();
        List<String> existingIds = new ArrayList<>();
        for (SignatureFacade s : signatures) {
            existingIds.add(s.getId().toUpperCase());
        }
        int id = 0;
        while (existingIds.contains("S" + id)) ++id;
        return "S" + id;
    }
}
