package ee.ria.scardcomlibrary;

import android.hardware.usb.UsbDevice;

public interface SmartCardReader {

    boolean supports(UsbDevice usbDevice);

    void close();
}
