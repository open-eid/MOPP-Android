package ee.ria.DigiDoc.android.signature.update.exception;

import android.content.Context;

import ee.ria.DigiDoc.common.exception.SignatureUpdateError;

public class PINException extends Exception implements SignatureUpdateError {

  public PINException(String message) {
    super(message);
  }

  @Override
  public String getMessage(Context context) {
    return super.getMessage();
  }
}
