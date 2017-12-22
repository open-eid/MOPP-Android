package ee.ria.DigiDoc.android.signature.data;

import android.support.annotation.StringDef;

import static ee.ria.DigiDoc.android.signature.data.SignatureAddStatus.CHECK_CERTIFICATE;
import static ee.ria.DigiDoc.android.signature.data.SignatureAddStatus.GOT_SIGNATURE;
import static ee.ria.DigiDoc.android.signature.data.SignatureAddStatus.REQUEST_PENDING;
import static ee.ria.DigiDoc.android.signature.data.SignatureAddStatus.REQUEST_SENT;

@StringDef({CHECK_CERTIFICATE, REQUEST_SENT, REQUEST_PENDING, GOT_SIGNATURE})
public @interface SignatureAddStatus {

    String CHECK_CERTIFICATE = "CHECK_CERTIFICATE";
    String REQUEST_SENT = "REQUEST_SENT";
    String REQUEST_PENDING = "REQUEST_PENDING";
    String GOT_SIGNATURE = "GOT_SIGNATURE";
}
