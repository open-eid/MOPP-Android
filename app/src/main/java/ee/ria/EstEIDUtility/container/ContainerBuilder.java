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

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.Container;

public class ContainerBuilder {

    public enum ContainerLocation {
        CACHE
    }

    private Context context;
    private ContainerLocation containerLocation;
    private String containerName;
    private File containerFile;
    private List<Uri> dataFileUris = new ArrayList<>();

    private ContainerBuilder() {}

    public ContainerFacade build() {
        if (containerFile == null) {
            containerFile = resolveContainerFile();
        }
        Container container = getContainer();
        for (Uri uri : dataFileUris) {
            File file = FileUtils.cacheUriAsDataFile(context, uri);
            String mimeType = resolveMimeType(file);
            if (mimeType == null) {
                if (FileUtils.isContainer(file.getName())) {
                    mimeType = "application/zip";
                } else {
                    continue;
                }
            }
            container.addDataFile(file.getAbsolutePath(), mimeType);
        }
        if (!dataFileUris.isEmpty()) {
            container.save(containerFile.getAbsolutePath());
        }
        return new ContainerFacade(container, containerFile);
    }

    private Container getContainer() {
        if (containerFile.exists()) {
            return Container.open(containerFile.getAbsolutePath());
        }
        return Container.create(containerFile.getAbsolutePath());
    }

    private String resolveMimeType(File file) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(file.getName()));
    }

    private File resolveContainerFile() {
        File directory = resolveDirectory();
        String containerName = resolveContainerName();
        return new File(directory, containerName);
    }

    private String resolveContainerName() {
        return containerName != null && !containerName.isEmpty() ? containerName : UUID.randomUUID().toString();
    }

    private File resolveDirectory() {
        if (containerLocation == null || containerLocation == ContainerLocation.CACHE) {
            return FileUtils.getContainerCacheDirectory(context);
        } else {
            return FileUtils.getContainersDirectory(context);
        }
    }

    public static ContainerBuilder aContainer(Context context) {
        ContainerBuilder cb = new ContainerBuilder();
        cb.context = context;
        return cb;
    }

    public ContainerBuilder fromExistingContainer(File containerFile) {
        this.containerFile = containerFile;
        return this;
    }

    public ContainerBuilder fromExistingContainer(Uri uri) {
        return fromExistingContainer(FileUtils.cacheUriAsContainerFile(context, uri));
    }

    public ContainerBuilder fromExistingContainer(String containerFilePath) {
        return fromExistingContainer(new File(containerFilePath));
    }

    public ContainerBuilder withDataFile(Uri uri) {
        dataFileUris.add(uri);
        return this;
    }

    public ContainerBuilder withDataFiles(List<Uri> uris) {
        dataFileUris.addAll(uris);
        return this;
    }

    public ContainerBuilder withContainerLocation(ContainerLocation containerLocation) {
        this.containerLocation = containerLocation;
        return this;
    }

    public ContainerBuilder withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }
}
