package ee.ria.EstEIDUtility.domain;

public class ContainerInfo {

    private String name;
    private String created;

    public ContainerInfo(String name, String created) {
        this.name = name;
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public String getCreated() {
        return created;
    }
}
