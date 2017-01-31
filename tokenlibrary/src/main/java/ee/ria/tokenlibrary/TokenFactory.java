/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.tokenlibrary;

import android.util.Log;

import ee.ria.scardcomlibrary.CardReader;
import ee.ria.tokenlibrary.util.TokenVersion;
import ee.ria.tokenlibrary.util.Util;

public class TokenFactory {

    private static final String TAG = TokenFactory.class.getName();
    public static Token getTokenImpl(CardReader cardReader) {

        String cardVersion = null;
        try {
            byte[] versionBytes = cardReader.transmitExtended(new byte[]{0x00, (byte) 0xCA, 0x01, 0x00, 0x03});
            cardVersion = Util.toHex(versionBytes);
        } catch (Exception e) {
            Log.e(TAG, "getTokenImpl: ", e);
        }

        if (cardVersion == null) {
            return null;
        }
        Token token = null;
        if (cardVersion.startsWith(TokenVersion.V3D5.getVersion())) {
            token = new EstEIDv3d5(cardReader);
        } else if (cardVersion.startsWith(TokenVersion.V3D4.getVersion()) || cardVersion.startsWith(TokenVersion.V3D0.getVersion())) {
            token = new EstEIDv3d4(cardReader);
        }
        return token;
    }

}
