package ee.ria.token.tokenservice.util;

import ee.ria.token.tokenservice.SMInterface;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.impl.EstEIDv3d4;
import ee.ria.token.tokenservice.impl.EstEIDv3d5;

public class TokenFactory {

    public static Token getTokenImpl(byte[] versionBytes, SMInterface sminterface) {
        String cardVersion = Util.toHex(versionBytes);

        Token token = null;
        if (cardVersion.startsWith(TokenVersion.V3D5.getVersion())) {
            token = new EstEIDv3d5(sminterface);
        } else if (cardVersion.startsWith(TokenVersion.V3D4.getVersion()) || cardVersion.startsWith(TokenVersion.V3D0.getVersion())) {
            token = new EstEIDv3d4(sminterface);
        }
        return token;
    }

}
