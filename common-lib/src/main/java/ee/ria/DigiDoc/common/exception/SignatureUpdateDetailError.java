package ee.ria.DigiDoc.common.exception;

import android.content.Context;
import android.text.Spanned;

public interface SignatureUpdateDetailError extends SignatureUpdateError {
    Spanned getDetailMessage(Context context);
}
