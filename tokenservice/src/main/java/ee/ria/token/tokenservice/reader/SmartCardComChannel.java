package ee.ria.token.tokenservice.reader;

public interface SmartCardComChannel {

    boolean isSecureChannel();
    byte[] transmit(byte[] apdu);
    byte[] transmitExtended(byte[] apdu);
}
