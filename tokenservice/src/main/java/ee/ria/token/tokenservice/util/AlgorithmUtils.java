package ee.ria.token.tokenservice.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AlgorithmUtils {

    private static final int BINARY_SHA1_LENGTH = 20;
    private static final int BINARY_SHA224_LENGTH = 28;
    private static final int BINARY_SHA256_LENGTH = 32;
    private static final int BINARY_SHA384_LENGTH = 48;
    private static final int BINARY_SHA512_LENGTH = 64;

    private enum ALGORITHM {
        SHA_1(new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14}),
        SHA_224(new byte[]{0x30, 0x2D, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x04, 0x05, 0x00, 0x04, 0x1C}),
        SHA_256(new byte[]{0x30, 0x31, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20}),
        SHA_384(new byte[]{0x30, 0x41, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x02, 0x05, 0x00, 0x04, 0x30}),
        SHA_512(new byte[]{0x30, 0x51, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x05, 0x00, 0x04, 0x40});

        private byte[] padding;

        ALGORITHM(byte[] padding) {
            this.padding = padding;
        }
    }

    public static byte[] addPadding(byte[] hash) {
        try (ByteArrayOutputStream toSign = new ByteArrayOutputStream()) {
            toSign.write(getAlgorithm(hash.length).padding);
            toSign.write(hash);
            return toSign.toByteArray();
        } catch (IOException e) {
            throw new HashPaddingFailedException(e);
        }
    }

    private static ALGORITHM getAlgorithm(int hashLength) {
        switch (hashLength) {
            case BINARY_SHA1_LENGTH: return ALGORITHM.SHA_1;
            case BINARY_SHA224_LENGTH : return ALGORITHM.SHA_224;
            case BINARY_SHA256_LENGTH : return ALGORITHM.SHA_256;
            case BINARY_SHA384_LENGTH : return ALGORITHM.SHA_384;
            case BINARY_SHA512_LENGTH : return ALGORITHM.SHA_512;
            default:
                throw new UnsupportedAlgorithmException();
        }
    }


    private static class UnsupportedAlgorithmException extends HashPaddingFailedException {
        UnsupportedAlgorithmException() {
            super("Unsupported Algorithm");
        }
    }

    private static class HashPaddingFailedException extends RuntimeException {
        HashPaddingFailedException(String message) {
            super(message);
        }

        HashPaddingFailedException(Exception cause) {
            super("Failed to Add padding to hash", cause);
        }
    }
}
