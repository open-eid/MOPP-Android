package ee.ria.scardcomlibrary.acs;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.acs.smartcard.Reader;

import ee.ria.scardcomlibrary.SmartCardReader;

public final class AcsSmartCardReader implements SmartCardReader {

    private final Reader reader;

    public AcsSmartCardReader(UsbManager usbManager) {
        reader = new Reader(usbManager);
    }

    @Override
    public boolean supports(UsbDevice usbDevice) {
        return reader.isSupported(usbDevice);
    }

    @Override
    public void close() {}
}
