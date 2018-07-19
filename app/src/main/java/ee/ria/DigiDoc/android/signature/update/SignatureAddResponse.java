package ee.ria.DigiDoc.android.signature.update;

import android.support.annotation.Nullable;

import ee.ria.DigiDoc.sign.data.SignedContainer;

public interface SignatureAddResponse {

    @Nullable SignedContainer container();

    boolean active();

    boolean showDialog();

    SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous);
}
