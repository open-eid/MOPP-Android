package ee.ria.EstEIDUtility.domain;

public class BdocItem {

    private String name;
    private String created;

    public BdocItem(String name, String created) {
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
