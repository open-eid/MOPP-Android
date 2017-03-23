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

import android.content.Context;
import android.net.Uri;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ee.ria.DigiDoc.preferences.accessor.AppPreferences;
import ee.ria.DigiDoc.util.FileUtils;
import ee.ria.libdigidocpp.Container;

public class ContainerBuilder {

    private Context context;
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
            String mimeType = FileUtils.resolveMimeType(file.getName());
            if (mimeType == null) {
                continue;
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

    private File resolveContainerFile() {
        File directory = FileUtils.getContainersDirectory(context);
        String containerName = resolveContainerName();
        return FileUtils.incrementFileName(directory, containerName);
    }

    private String resolveContainerName() {
        if (containerName != null && !containerName.isEmpty()) {
            return containerName;
        }
        else if(!dataFileUris.isEmpty()) {
            return FilenameUtils.getBaseName(FileUtils.resolveFileName(dataFileUris.get(0), context.getContentResolver()))
                    + "." + AppPreferences.get(context).getContainerFormat();
        }
        return UUID.randomUUID().toString();
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

    public ContainerBuilder fromExistingContainer(String containerFilePath) {
        return fromExistingContainer(new File(containerFilePath));
    }

    public ContainerBuilder fromExternalContainer(Uri uri) throws ExternalContainerSaveException {
        File from = FileUtils.uriAsContainerFile(context, uri);
        String containerName = from.getName();
        File directory = FileUtils.getContainersDirectory(context);

        File to = FileUtils.incrementFileName(directory, containerName);
        try {
            FileChannel outputChannel = new FileOutputStream(to).getChannel();
            FileChannel inputChannel = new FileInputStream(from).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            from.delete();
        } catch (Exception e) {
            throw new ExternalContainerSaveException();
        }
        this.containerFile = to;
        return this;
    }

    public ContainerBuilder withDataFile(Uri uri) {
        dataFileUris.add(uri);
        return this;
    }

    public ContainerBuilder withDataFiles(List<Uri> uris) {
        dataFileUris.addAll(uris);
        return this;
    }

    public ContainerBuilder withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    public class ExternalContainerSaveException extends Exception {
    }
}
