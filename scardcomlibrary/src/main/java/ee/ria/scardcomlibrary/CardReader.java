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

package ee.ria.scardcomlibrary;

import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.Arrays;

import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.scardcomlibrary.impl.Identive;
import ee.ria.scardcomlibrary.impl.Omnikey;


public abstract class CardReader implements SmartCardComChannel {

    protected static final String TAG = CardReader.class.getName();
    protected static final String ACTION_USB_PERMISSION = "ee.ria.token.tokenservice.USB_PERMISSION";

    public abstract void connect(Connected connected);

    public abstract void close();

    public enum Provider {
        ACS, IDENTIVE, OMNIKEY
    }

    public BroadcastReceiver receiver;
    public BroadcastReceiver usbAttachReceiver;
    public BroadcastReceiver usbDetachReceiver;

    protected static abstract class Connected {
        public abstract void connected();
    }

    public static CardReader getInstance(Context context, Provider provider) {
        switch (provider) {
            case ACS:
                return new ACS(context);
            case IDENTIVE:
                Identive identive = new Identive(context);
                if (identive.hasSupportedReader()) {
                    return identive;
                }
                break;
            case OMNIKEY:
                Omnikey omnikey = new Omnikey(context);
                if (omnikey.hasSupportedReader()) {
                    return omnikey;
                }
                break;
        }
        throw new IllegalArgumentException("Unknown provider " + provider);
    }

    @Override
    public byte[] transmitExtended(byte[] apdu) {
        byte[] recv = transmit(apdu);
        byte sw1 = recv[recv.length - 2];
        byte sw2 = recv[recv.length - 1];
        recv = Arrays.copyOf(recv, recv.length - 2);
        if (sw1 == 0x61) {
            recv = concat(recv, transmit(new byte[]{0x00, (byte) 0xC0, 0x00, 0x00, sw2}));
        } else if (!(sw1 == (byte) 0x90 && sw2 == (byte) 0x00)) {
            throw new ScardOperationUnsuccessfulException("SW != 9000");
        }
        return recv;
    }

    private static byte[] concat(byte[]... arrays) {
        int size = 0;
        for (byte[] array : arrays) {
            size += array.length;
        }
        byte[] result = new byte[size];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }
}
