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

package ee.ria.token.tokenservice.reader.impl;

import android.content.Context;
import android.util.Log;

import com.identive.libs.SCard;
import com.identive.libs.WinDefs;

import java.util.ArrayList;
import java.util.Arrays;

import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.reader.SmartCardCommunicationException;

public class Identive extends CardReader {

    private SCard ctx;
    private Context context;

    public Identive(Context context) {
        this.context = context;
        ctx = new SCard();
        ctx.SCardEstablishContext(context);
    }

    @Override
    public void close() {
        ctx.SCardDisconnect(WinDefs.SCARD_LEAVE_CARD);
        ctx.SCardReleaseContext();
    }

    public boolean hasSupportedReader() {
        try {
            ArrayList<String> deviceList = new ArrayList<>();
            ctx.USBRequestPermission(context);
            ctx.SCardListReaders(context, deviceList);
            return deviceList.size() > 0;
        } catch (Exception e) {
            Log.e(TAG, "hasSupportedReader: ", e);
        }
        return false;
    }

    @Override
    public void connect(Connected connected) {
        ArrayList<String> deviceList = new ArrayList<>();
        ctx.SCardListReaders(context, deviceList);
        ctx.SCardConnect(deviceList.get(0), WinDefs.SCARD_SHARE_EXCLUSIVE,
                (int) (WinDefs.SCARD_PROTOCOL_T0 | WinDefs.SCARD_PROTOCOL_T1));
        connected.connected();
    }

    @Override
    public boolean isSecureChannel() {
        return true;
    }

    @Override
    public byte[] transmit(byte[] apdu) {
        SCard.SCardIOBuffer io = ctx.new SCardIOBuffer();
        io.setAbyInBuffer(apdu);
        io.setnBytesReturned(apdu.length);
        io.setAbyOutBuffer(new byte[0x8000]);
        io.setnOutBufferSize(0x8000);
        ctx.SCardTransmit(io);
        if (io.getnBytesReturned() == 0) {
            throw new SmartCardCommunicationException("Failed to send apdu");
        }
        String rstr = "";
        for (int k = 0; k < io.getnBytesReturned(); k++) {
            int temp = io.getAbyOutBuffer()[k] & 0xFF;
            if (temp < 16) {
                rstr = rstr.toUpperCase() + "0" + Integer.toHexString(io.getAbyOutBuffer()[k]);
            } else {
                rstr = rstr.toUpperCase() + Integer.toHexString(temp);
            }
        }
        return Arrays.copyOf(io.getAbyOutBuffer(), io.getnBytesReturned());
    }
}
