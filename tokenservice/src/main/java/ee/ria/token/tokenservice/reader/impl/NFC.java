package ee.ria.token.tokenservice.reader.impl;


import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;

import ee.ria.token.tokenservice.reader.CardReader;

public class NFC extends CardReader {
    private IsoDep nfc;

    public NFC(Tag tag) {
        nfc = IsoDep.get(tag);
    }

    @Override
    public byte[] transmit(byte[] apdu) throws Exception {
        return nfc.transceive(apdu);
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
