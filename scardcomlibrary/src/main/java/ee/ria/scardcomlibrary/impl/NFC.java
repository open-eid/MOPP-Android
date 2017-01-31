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

package ee.ria.scardcomlibrary.impl;


import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;

import ee.ria.scardcomlibrary.CardReader;
import ee.ria.scardcomlibrary.SmartCardCommunicationException;

public class NFC extends CardReader {
    private IsoDep nfc;

    public NFC(Tag tag) {
        nfc = IsoDep.get(tag);
    }

    @Override
    public boolean isSecureChannel() {
        return false;
    }

    @Override
    public byte[] transmit(byte[] apdu) {
        try {
            return nfc.transceive(apdu);
        } catch (IOException e) {
            throw new SmartCardCommunicationException(e);
        }
    }

    @Override
    public void connect(Connected connected) {
        try {
            nfc.connect();
            nfc.setTimeout(5000);
            connected.connected();
        } catch(IOException e) {
            Log.e(TAG, "connect: ", e);
        }
    }

    @Override
    public void close() {
        try {
            nfc.close();
        } catch (IOException e) {
            Log.e(TAG, "close: ", e);
        }
    }
}
