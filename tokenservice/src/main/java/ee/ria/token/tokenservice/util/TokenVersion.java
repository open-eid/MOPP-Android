package ee.ria.token.tokenservice.util;

public enum TokenVersion {
    V3D5("0305"), V3D4("0304"), V3D0("0300");

    private String version;
    TokenVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
