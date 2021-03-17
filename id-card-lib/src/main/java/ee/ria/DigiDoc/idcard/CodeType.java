package ee.ria.DigiDoc.idcard;

public enum CodeType {

    PIN1((byte) 0x01, (byte) 0x01),
    PIN2((byte) 0x02, (byte) 0x02),
    PUK((byte) 0x00, (byte) 0x03);

    public final byte value;
    public final byte retryValue;

    CodeType(byte value, byte retryValue) {
        this.value = value;
        this.retryValue = retryValue;
    }
}
