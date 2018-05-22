package ee.ria.scardcomlibrary;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public final class SmartCardReaderManager {

    /**
     * Card connected retry interval in milliseconds.
     */
    private static final int CONNECT_RETRY = 2500;

    private final Context context;
    private final UsbManager usbManager;
    private final ImmutableList<SmartCardReader> readers;

    public SmartCardReaderManager(Context context, UsbManager usbManager,
                                  ImmutableList<SmartCardReader> readers) {
        this.context = context;
        this.usbManager = usbManager;
        this.readers = readers;
    }

    public boolean supports(UsbDevice usbDevice) {
        return reader(usbDevice) != null;
    }

    @Nullable
    public SmartCardReader reader(UsbDevice usbDevice) {
        for (SmartCardReader reader : readers) {
            if (reader.supports(usbDevice)) {
                if (usbManager.hasPermission(usbDevice)) {
                    reader.open(usbDevice);
                }
                return reader;
            }
        }
        return null;
    }

    public Observable<Optional<SmartCardReader>> reader() {
        return Observable
                .create(new SmartCardReaderOnSubscribe(context, this))
                .switchMap(readerOptional -> {
                    if (readerOptional.isPresent()) {
                        return Observable
                                .fromCallable(() -> {
                                    readerOptional.get().connected();
                                    return readerOptional;
                                })
                                .repeatWhen(completed ->
                                        completed.delay(CONNECT_RETRY, TimeUnit.MILLISECONDS));
                    } else {
                        return Observable.just(readerOptional);
                    }
                });
    }
}
