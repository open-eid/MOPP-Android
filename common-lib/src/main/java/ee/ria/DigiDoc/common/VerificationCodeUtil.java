package ee.ria.DigiDoc.common;


import org.bouncycastle.util.encoders.Base64;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class VerificationCodeUtil {

    private VerificationCodeUtil() {
    }

    public static String calculateSmartIdVerificationCode(String base64Hash) throws NoSuchAlgorithmException {
        byte[] hash = Base64.decode(base64Hash);
        byte[] codeDigest = MessageDigest.getInstance("SHA-256").digest(hash);
        String code = String.valueOf(ByteBuffer.wrap(codeDigest).getShort(codeDigest.length - 2) & 0xffff);
        String paddedCode = "0000" + code;
        return paddedCode.substring(code.length());
    }

    public static String calculateMobileIdVerificationCode(byte[] hash) {
        return String.format(Locale.ROOT, "%04d", hash != null ? ((0xFC & hash[0]) << 5) | (hash[hash.length - 1] & 0x7F) : 0);
    }

}
