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

import ee.ria.scardcomlibrary.SmartCardReader;
import ee.ria.tokenlibrary.util.TokenVersion;
import ee.ria.tokenlibrary.util.Util;
import timber.log.Timber;

public class TokenFactory {

    public static Token getTokenImpl(SmartCardReader reader) {
        String cardVersion = null;
        try {
            byte[] versionBytes = reader.transmit(
                    (byte) 0x00, (byte) 0xCA, (byte) 0x01, (byte) 0x00, null, 0x03);
            cardVersion = Util.toHex(versionBytes);
        } catch (Exception e) {
            Timber.e(e, "Error retrieving token implementation");
        }

        if (cardVersion == null) {
            return null;
        }
        Token token = null;
        if (cardVersion.startsWith(TokenVersion.V3D5.getVersion())) {
            token = new EstEIDv3d5(reader);
        } else if (cardVersion.startsWith(TokenVersion.V3D4.getVersion()) || cardVersion.startsWith(TokenVersion.V3D0.getVersion())) {
            token = new EstEIDv3d4(reader);
        }
        return token;
    }
}
