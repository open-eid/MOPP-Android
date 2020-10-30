package ee.ria.DigiDoc.android.signature.update;

import androidx.annotation.Nullable;

import ee.ria.DigiDoc.sign.SignedContainer;

public interface SignatureAddResponse {

    @Nullable SignedContainer container();

    boolean active();

    boolean showDialog();

    SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous);
}
