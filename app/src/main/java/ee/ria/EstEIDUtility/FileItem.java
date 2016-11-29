package ee.ria.EstEIDUtility;


public class FileItem {
    private String name;
    private String path;
    private long timestamp;

    public FileItem(String name, String path, long timestamp) {
        this.name = name;
        this.path = path;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return path;
    }

    public void setLocation(String location) {
        this.path = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


}
