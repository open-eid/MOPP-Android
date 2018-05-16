package ee.ria.scardcomlibrary;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import io.reactivex.Observable;

public final class SmartCardReaderManager {

    private final Context context;
    private final ImmutableList<SmartCardReader> readers;

    public SmartCardReaderManager(Context context, ImmutableList<SmartCardReader> readers) {
        this.context = context;
        this.readers = readers;
    }

    public boolean supports(UsbDevice usbDevice) {
        return reader(usbDevice) != null;
    }

    @Nullable
    public SmartCardReader reader(UsbDevice usbDevice) {
        for (SmartCardReader reader : readers) {
            if (reader.supports(usbDevice)) {
                return reader;
            }
        }
        return null;
    }

    public Observable<Optional<SmartCardReader>> reader() {
        return Observable.create(new SmartCardReaderOnSubscribe(context, this));
    }
}
