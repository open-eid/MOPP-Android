package ee.ria.scardcomlibrary.identiv;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.identive.libs.SCard;

import java.util.ArrayList;

import ee.ria.scardcomlibrary.SmartCardReader;

public final class IdentivSmartCardReader implements SmartCardReader {

    private static final int VENDOR_ID = 1254;

    private final Context context;
    private final UsbManager usbManager;
    private final SCard sCard;

    public IdentivSmartCardReader(Context context, UsbManager usbManager) {
        this.context = context;
        this.usbManager = usbManager;
        sCard = new SCard();
        sCard.SCardEstablishContext(context);
    }

    @Override
    public boolean supports(UsbDevice usbDevice) {
        if (!usbManager.hasPermission(usbDevice) && usbDevice.getVendorId() == VENDOR_ID) {
            return true;
        }
        ArrayList<String> readers = new ArrayList<>();
        sCard.SCardListReaders(context, readers);
        return readers.size() > 0;
    }

    @Override
    public void close() {
        sCard.SCardReleaseContext();
    }
}
