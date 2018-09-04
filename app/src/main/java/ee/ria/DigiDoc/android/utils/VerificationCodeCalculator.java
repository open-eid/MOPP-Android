package ee.ria.DigiDoc.android.utils;


import org.bouncycastle.jcajce.provider.digest.SHA256;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class VerificationCodeCalculator {
    public static String calculate(byte[] documentHash) {
        MessageDigest messageDigest = new SHA256.Digest();
        byte[] digest = messageDigest.digest(documentHash);
        ByteBuffer byteBuffer = ByteBuffer.wrap(digest);
        int shortBytes = Short.SIZE / Byte.SIZE; // Short.BYTES in java 8
        int rightMostBytesIndex = byteBuffer.limit() - shortBytes;
        short twoRightmostBytes = byteBuffer.getShort(rightMostBytesIndex);
        int positiveInteger = ((int) twoRightmostBytes) & 0xffff;
        String code = String.valueOf(positiveInteger);
        String paddedCode = "0000" + code;
        return paddedCode.substring(code.length());
    }
}
