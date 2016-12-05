package ee.ria.token.tokenservice.reader.impl;

import android.content.Context;
import android.util.Log;

import com.identive.libs.SCard;
import com.identive.libs.WinDefs;

import java.util.ArrayList;
import java.util.Arrays;

import ee.ria.token.tokenservice.reader.CardReader;

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
    public byte[] transmit(byte[] apdu) throws Exception {
        SCard.SCardIOBuffer io = ctx.new SCardIOBuffer();
        io.setAbyInBuffer(apdu);
        io.setnBytesReturned(apdu.length);
        io.setAbyOutBuffer(new byte[0x8000]);
        io.setnOutBufferSize(0x8000);
        ctx.SCardTransmit(io);
        if (io.getnBytesReturned() == 0) {
            throw new Exception("Failed to send apdu");
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
