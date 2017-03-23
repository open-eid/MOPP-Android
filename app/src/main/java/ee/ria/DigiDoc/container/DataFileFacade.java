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


import ee.ria.libdigidocpp.DataFile;

public class DataFileFacade {

    private DataFile containerDataFile;

    DataFileFacade(DataFile containerDataFile) {
        this.containerDataFile = containerDataFile;
    }

    public String getFileName() {
        return containerDataFile.fileName();
    }

    public long getFileSize() {
        return containerDataFile.fileSize();
    }

    public String getId() {
        return containerDataFile.id();
    }

    public String getMediaType() {
        return containerDataFile.mediaType();
    }

    public byte[] calcDigest(String digestMethod) {
        return containerDataFile.calcDigest(digestMethod);
    }

    public void saveFile(String path) {
        containerDataFile.saveAs(path);
    }
}
