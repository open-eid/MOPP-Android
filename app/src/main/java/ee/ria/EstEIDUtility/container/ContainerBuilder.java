package ee.ria.EstEIDUtility.container;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.Container;

public class ContainerBuilder {

    public enum ContainerLocation {
        CACHE, STORAGE
    }

    private Context context;
    private ContainerLocation containerLocation;
    private String containerName;
    private Container container;
    private File containerFile;
    private List<Uri> dataFileUris = new ArrayList<>();

    private ContainerBuilder() {}

    public ContainerFacade build() {
        if (containerFile == null) {
            containerFile = resolveContainerFile();
        }
        container = getContainer();
        for (Uri uri : dataFileUris) {
            File file = FileUtils.cacheUriAsDataFile(context, uri);
            container.addDataFile(file.getAbsolutePath(), resolveMimeType(file));
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

    public ContainerBuilder withContainerLocation(ContainerLocation containerLocation) {
        this.containerLocation = containerLocation;
        return this;
    }

    public ContainerBuilder withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }
}
