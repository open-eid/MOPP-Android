package ee.ria.scardcomlibrary.acs;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

import java.util.Arrays;

import ee.ria.scardcomlibrary.SmartCardReader;
import ee.ria.scardcomlibrary.SmartCardReaderException;
import timber.log.Timber;

public final class AcsSmartCardReader extends SmartCardReader {

    private static final int SLOT = 0;

    private final Reader reader;

    public AcsSmartCardReader(UsbManager usbManager) {
        reader = new Reader(usbManager);
    }

    @Override
    public boolean supports(UsbDevice usbDevice) {
        return reader.isSupported(usbDevice);
    }

    @Override
    public void open(UsbDevice usbDevice) {
        reader.open(usbDevice);
    }

    @Override
    public void close() {
        reader.close();
    }

    @Override
    public boolean connected() {
        if (reader.isOpened() && reader.getState(SLOT) == Reader.CARD_PRESENT) {
            try {
                reader.power(SLOT, Reader.CARD_WARM_RESET);
                reader.setProtocol(SLOT, Reader.PROTOCOL_TX);
            } catch (ReaderException e) {
                Timber.e(e, "Connecting to ACS reader");
            }
        }
        return reader.isOpened() && reader.getState(SLOT) == Reader.CARD_SPECIFIC;
    }

    @Override
    public boolean isSecureChannel() {
        return true;
    }

    @Override
    protected byte[] transmit(byte[] apdu) throws SmartCardReaderException {
        byte[] recv = new byte[1024];
        int len;
        try {
            len = reader.transmit(SLOT, apdu, apdu.length, recv, recv.length);
        } catch (ReaderException e) {
            throw new SmartCardReaderException(e);
        }
        return Arrays.copyOf(recv, len);
    }
}
