package ee.ria.EstEIDUtility.domain;

import java.io.File;

public class ContainerInfo {

    private File path;
    private String name;
    private String created;

    public ContainerInfo(File path, String created) {
        this.path = path;
        this.name = path.getName();
        this.created = created;
    }

    public File getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getCreated() {
        return created;
    }
}
