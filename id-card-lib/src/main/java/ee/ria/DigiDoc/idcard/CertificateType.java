package ee.ria.DigiDoc.idcard;

public enum CertificateType {

    AUTHENTICATION((byte) 0xAA),
    SIGNING((byte) 0xDD);

    public final byte value;

    CertificateType(byte value) {
        this.value = value;
    }
}
