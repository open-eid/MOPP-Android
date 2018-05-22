package ee.ria.scardcomlibrary;

import android.hardware.usb.UsbDevice;

public interface SmartCardReader extends AutoCloseable {

    boolean supports(UsbDevice usbDevice);

    void open(UsbDevice usbDevice);

    boolean connected();
}
