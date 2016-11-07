package ee.ria.EstEIDUtility;


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

    public void setName(String name) {
        this.name = name;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }
}
