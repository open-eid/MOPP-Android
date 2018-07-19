package ee.ria.DigiDoc.sign;

import android.app.Application;
import android.support.test.InstrumentationRegistry;

public final class AndroidTestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MoppLib.init(InstrumentationRegistry.getContext());
    }
}
