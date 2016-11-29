package ee.ria.token.tokenservice.util;

public enum TokenVersion {
    v3d5("0305"), v3d4("0304");

    private String version;
    TokenVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
