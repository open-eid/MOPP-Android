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

import org.apache.commons.io.FilenameUtils;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.style.BCStyle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ee.ria.DigiDoc.certificate.X509Cert;
import ee.ria.DigiDoc.util.FileUtils;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.Signatures;

public class ContainerFacade {

    private Container container;
    private File containerFile;
    private Signature preparedSignature;
    private static final List<String> isRWContainer = Arrays.asList("asice", "sce", "bdoc", "edoc");

    public ContainerFacade(Container container, File containerFile) {
        this.container = container;
        this.containerFile = containerFile;
    }

    public File getContainerFile() {
        return containerFile;
    }

    public void addDataFile(File dataFile) {
        if (hasDataFile(dataFile.getName())) {
            throw new DataFileWithSameNameAlreadyExistsException();
        }
        String mimeType = FileUtils.resolveMimeType(dataFile.getName());
        container.addDataFile(dataFile.getAbsolutePath(), mimeType);
        save();
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

    public SignatureFacade getLastSignature() {
        List<SignatureFacade> signatures = getSignatures();
        return signatures.isEmpty() ? null : signatures.get(signatures.size() -1);
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

    private void save(File filePath) {
        container.save(filePath.getAbsolutePath());
        containerFile = filePath;
    }

    public boolean isSigned() {
        return !container.signatures().isEmpty();
    }

    public boolean isRWContainer() {
        return isRWContainer.contains(FilenameUtils.getExtension(containerFile.getName()));
    }

    public void removeDataFile(int position) {
        container.removeDataFile(position);
        save();
    }

    public void removeSignature(int position) {
        container.removeSignature(position);
        save();
    }

    public boolean isSignedBy(byte[] cert) {
        return isSignedBy(new X509Cert(cert).getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.SERIALNUMBER)));
    }

    public boolean isSignedBy(String personalCode) {
        for (SignatureFacade signatureFacade : getSignatures()) {
            String serialNumber = signatureFacade.getSigningCertificate()
                    .getValueByObjectIdentifier(ASN1ObjectIdentifier.getInstance(BCStyle.SERIALNUMBER));
            if (personalCode != null && serialNumber != null && personalCode.equals(serialNumber)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDataFiles() {
        return !getDataFiles().isEmpty();
    }

    public byte[] prepareWebSignature(byte[] cert, String profile) {
        preparedSignature = container.prepareWebSignature(cert, profile);
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

    public SignatureFacade getPreparedSignature() {
        return new SignatureFacade(preparedSignature);
    }

    public String getExtendedSignatureProfile() {
        if (isSignedWithExtendedProfile()) {
            String profile = getFirstSignatureProfile();
            profile = profile.substring(profile.lastIndexOf("/") + 1);
            return profile;
        }
        return null;
    }

    private boolean isSignedWithExtendedProfile() {
        if (!isSigned()) {
            return false;
        }
        String profile = getFirstSignatureProfile();
        profile = profile.substring(profile.lastIndexOf("/") + 1);
        for (String option : Arrays.asList("time-stamp", "time-mark")) {
            if (profile.contains(option)) {
                return true;
            }
        }
        return false;
    }

    private String getFirstSignatureProfile() {
        return container.signatures().get(0).profile();
    }

    public void addAdESSignature(byte[] encoded) {
        container.addAdESSignature(encoded);
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

    public class DataFileWithSameNameAlreadyExistsException extends RuntimeException {
    }
}
