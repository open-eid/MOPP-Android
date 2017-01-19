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

package ee.ria.EstEIDUtility.container;

import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.ria.EstEIDUtility.domain.X509Cert;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.Signatures;

public class ContainerFacade {

    private Container container;
    private File containerFile;
    private Map<String, File> dataFileLocations = new HashMap<>();
    private Signature preparedSignature;

    ContainerFacade(Container container, File containerFile) {
        this.container = container;
        this.containerFile = containerFile;
    }

    public Container getContainer() {
        return container;
    }

    public File getContainerFile() {
        return containerFile;
    }

    public void addDataFile(File dataFile) {
        if (hasDataFile(dataFile.getName())) {
            throw new DataFileWithSameNameAlreadyExistsException();
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(dataFile.getName()));
        container.addDataFile(dataFile.getAbsolutePath(), mimeType);
        dataFileLocations.put(dataFile.getName(), dataFile);
        save();
    }

    public DataFileFacade getDataFile(String filename) {
        File file = dataFileLocations.get(filename);
        DataFile dataFile;
        if (file != null && (dataFile = getContainerDataFile(filename)) != null) {
            return new DataFileFacade(dataFile, file);
        }
        throw new DataFileDoesNotExistException();
    }

    public List<DataFile> getDataFiles() {
        if (container == null) {
            return Collections.emptyList();
        }
        List<DataFile> dataFiles = new ArrayList<>();
        DataFiles containerDataFiles = container.dataFiles();
        for (int i = 0; i < containerDataFiles.size(); i++) {
            dataFiles.add(containerDataFiles.get(i));
        }
        return dataFiles;
    }

    public List<Signature> getSignatures() {
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

    public String getName() {
        return containerFile.getName();
    }

    public String getAbsolutePath() {
        return containerFile.getAbsolutePath();
    }

    public long fileSize() {
        return containerFile.length();
    }

    public void save() {
        save(containerFile);
    }

    public void save(File filePath) {
        container.save(filePath.getAbsolutePath());
        containerFile = filePath;
    }

    public void save(String absolutePath) {
        container.save(absolutePath);
        containerFile = new File(absolutePath);
    }

    public boolean isSigned() {
        return !container.signatures().isEmpty();
    }

    public void removeDataFile(int position) {
        container.removeDataFile(position);
    }

    public void removeSignature(int position) {
        container.removeSignature(position);
    }

    public boolean isSignedBy(byte[] cert) {
        X509Cert x509Cert = new X509Cert(cert);
        for (Signature signature : getSignatures()) {
            X509Cert c = new X509Cert(signature.signingCertificateDer());
            if (c.getCertificate().equals(x509Cert.getCertificate())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDataFiles() {
        return !getDataFiles().isEmpty();
    }

    public byte[] prepareWebSignature(byte[] cert) {
        preparedSignature = getContainer().prepareWebSignature(cert);
        return preparedSignature.dataToSign();
    }

    public void setSignatureValue(byte[] signatureValue) {
        preparedSignature.setSignatureValue(signatureValue);
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

    private boolean hasDataFile(String attachedName) {
        DataFiles containerDataFiles = container.dataFiles();
        for (int i = 0; i < containerDataFiles.size(); i++) {
            DataFile dataFile = containerDataFiles.get(i);
            if (dataFile.fileName().equals(attachedName)) {
                return true;
            }
        }
        return false;
    }

    public Signature getPreparedSignature() {
        return this.preparedSignature;
    }

    public class DataFileDoesNotExistException extends RuntimeException {
    }

    public class DataFileWithSameNameAlreadyExistsException extends RuntimeException {
    }
}
