package ee.ria.token.tokenservice.reader;

public interface ScardComChannel {

    byte[] transmit(byte[] apdu) throws Exception;
    byte[] transmitExtended(byte[] apdu) throws Exception;
}
