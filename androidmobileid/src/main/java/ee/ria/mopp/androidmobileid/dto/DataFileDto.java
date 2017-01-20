package ee.ria.mopp.androidmobileid.dto;

public class DataFileDto {
    private String id;
    private String digestType;
    private String digestValue;
    private String mimeType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDigestType() {
        return digestType;
    }

    public void setDigestType(String digestType) {
        this.digestType = digestType;
    }

    public String getDigestValue() {
        return digestValue;
    }

    public void setDigestValue(String digestValue) {
        this.digestValue = digestValue;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataFileDto{");
        sb.append("id='").append(id).append('\'');
        sb.append(", digestType='").append(digestType).append('\'');
        sb.append(", digestValue='").append(digestValue).append('\'');
        sb.append(", mimeType='").append(mimeType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
