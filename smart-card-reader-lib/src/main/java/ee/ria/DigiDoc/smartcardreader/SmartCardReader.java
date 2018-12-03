package ee.ria.DigiDoc.smartcardreader;

import android.hardware.usb.UsbDevice;

import java.util.Arrays;

import timber.log.Timber;

import static com.google.common.primitives.Bytes.concat;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

/**
 * Base class for smart card readers.
 *
 * TODO Log all transmit commands in hex (request and response)
 */
public abstract class SmartCardReader implements AutoCloseable {

    public abstract boolean supports(UsbDevice usbDevice);

    public abstract void open(UsbDevice usbDevice);

    public abstract boolean connected();

    public abstract byte[] atr();

    /**
     * Makes the actual transaction, has to be implemented by specific readers.
     *
     * @param apdu APDU to send
     * @return Return bytes.
     * @throws SmartCardReaderException When something fails.
     */
    protected abstract byte[] transmit(byte[] apdu) throws SmartCardReaderException;

    /**
     * Transmit APDU to the smart card reader.
     *
     * Automatically handles message chaining for large data transmissions and
     * reading additional data for large responses.
     *
     * @return Return bytes.
     * @throws SmartCardReaderException When something fails.
     */
    public final byte[] transmit(int cla, int ins, int p1, int p2, byte[] data, Integer le)
            throws SmartCardReaderException {
        Timber.d("transmit: %s %s %s %s %s %s", cla, ins, p1, p2, Arrays.asList(data), le);

        byte[] response;
        if (data == null || data.length == 0) {
            response = transmit(appendLe(
                    new byte[] {(byte) cla, (byte) ins, (byte) p1, (byte) p2},
                    le));
        } else if (data.length < 256) {
            response = transmit(appendLe(
                    concat(
                            new byte[] {(byte) cla, (byte) ins, (byte) p1, (byte) p2,
                                    (byte) data.length},
                            data),
                    le));
        } else {
            int remaining = data.length;
            while (remaining >= 256) {
                transmit(appendLe(
                        concat(
                                new byte[] {0x10, (byte) ins, (byte) p1, (byte) p2, (byte) 0xFF},
                                copyOfRange(data, data.length - remaining,
                                        data.length - remaining + 255)),
                        le));
                remaining -= 255;
            }
            response = transmit(appendLe(
                    concat(
                            new byte[] {(byte) cla, (byte) ins, (byte) p1, (byte) p2,
                                    (byte) remaining},
                            copyOfRange(data, data.length - remaining, data.length)),
                    le));
        }

        byte sw1 = response[response.length - 2];
        byte sw2 = response[response.length - 1];
        if (sw1 == (byte) 0x90 && sw2 == 0x00) {
            return copyOf(response, response.length - 2);
        } else if (sw1 == 0x61) {
            return concat(
                    copyOf(response, response.length - 2),
                    transmit(0x00, 0xC0, 0x00, 0x00, null, (int) sw2));
        }
        throw new ApduResponseException(sw1, sw2);
    }

    private static byte[] appendLe(byte[] apdu, Integer le) {
        if (le == null) {
            return apdu;
        } else {
            return concat(apdu, new byte[] {le.byteValue()});
        }
    }
}
