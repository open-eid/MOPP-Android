package ee.ria.scardcomlibrary.identiv;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.identive.libs.SCard;

import java.util.ArrayList;

import ee.ria.scardcomlibrary.SmartCardReader;

import static com.identive.libs.WinDefs.SCARD_LEAVE_CARD;
import static com.identive.libs.WinDefs.SCARD_PROTOCOL_TX;
import static com.identive.libs.WinDefs.SCARD_SHARE_EXCLUSIVE;
import static com.identive.libs.WinDefs.SCARD_SPECIFIC;

public final class IdentivSmartCardReader implements SmartCardReader {

    private static final int VENDOR_ID = 1254;

    private final Context context;
    private final UsbManager usbManager;
    private final SCard sCard;

    public IdentivSmartCardReader(Context context, UsbManager usbManager) {
        this.context = context;
        this.usbManager = usbManager;
        sCard = new SCard();
    }

    @Override
    public boolean supports(UsbDevice usbDevice) {
        if (!usbManager.hasPermission(usbDevice) && usbDevice.getVendorId() == VENDOR_ID) {
            return true;
        }
        open(usbDevice);
        ArrayList<String> readers = new ArrayList<>();
        sCard.SCardListReaders(context, readers);
        return readers.size() > 0;
    }

    @Override
    public void open(UsbDevice usbDevice) {
        sCard.SCardEstablishContext(context);
    }

    @Override
    public void close() {
        sCard.SCardDisconnect(SCARD_LEAVE_CARD);
        sCard.SCardReleaseContext();
    }

    @Override
    public boolean connected() {
        ArrayList<String> readers = new ArrayList<>();
        sCard.SCardListReaders(context, readers);
        if (readers.size() > 0) {
            sCard.SCardConnect(readers.get(0), SCARD_SHARE_EXCLUSIVE, (int) SCARD_PROTOCOL_TX);
            SCard.SCardState state = sCard.new SCardState();
            sCard.SCardStatus(state);
            return state.getnState() == SCARD_SPECIFIC;
        }
        return false;
    }
}
