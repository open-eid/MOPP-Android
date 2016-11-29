package ee.ria.EstEIDUtility.service;

import android.app.Service;

public interface ServiceCreatedCallback {
    void created(Service tokenService);
    void failed();
    void disconnected();
}
