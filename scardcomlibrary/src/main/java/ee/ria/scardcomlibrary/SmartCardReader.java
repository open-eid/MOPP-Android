package ee.ria.scardcomlibrary;

import android.hardware.usb.UsbDevice;

import java.util.Arrays;

public interface SmartCardReader extends AutoCloseable {

    boolean supports(UsbDevice usbDevice);

    void open(UsbDevice usbDevice);

    boolean connected();

    boolean isSecureChannel();

    byte[] transmit(byte[] apdu);

    default byte[] transmitExtended(byte[] apdu) {
        byte[] recv = transmit(apdu);
        byte sw1 = recv[recv.length - 2];
        byte sw2 = recv[recv.length - 1];
        recv = Arrays.copyOf(recv, recv.length - 2);
        if (sw1 == 0x61) {
            recv = concat(recv, transmit(new byte[]{0x00, (byte) 0xC0, 0x00, 0x00, sw2}));
        } else if (!(sw1 == (byte) 0x90 && sw2 == (byte) 0x00)) {
            throw new SmartCardCommunicationException("SW != 9000");
        }
        return recv;
    }

    static byte[] concat(byte[]... arrays) {
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
