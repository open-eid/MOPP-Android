package ee.ria.token.tokenservice.callback;

import android.util.SparseArray;

public interface PersonalFileCallback {
    void onPersonalFileResponse(SparseArray<String> result);
    void onPersonalFileError(String msg);
}
