package ee.ria.scardcomlibrary;

import java.util.Objects;

public class ApduResponseException extends SmartCardReaderException {

    private final byte sw1;
    private final byte sw2;

    ApduResponseException(byte sw1, byte sw2) {
        super("APDU error response sw1=" + sw1 + ", sw2=" + sw2);
        this.sw1 = sw1;
        this.sw2 = sw2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApduResponseException that = (ApduResponseException) o;
        return sw1 == that.sw1 &&
                sw2 == that.sw2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sw1, sw2);
    }

    @Override
    public String toString() {
        return "ApduResponseException{" +
                "sw1=" + sw1 +
                ", sw2=" + sw2 +
                '}';
    }
}
