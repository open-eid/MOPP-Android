package ee.ria.DigiDoc.common.exception;

import android.content.Context;

import ee.ria.DigiDoc.common.R;

public class InvalidProxySettingsException extends Exception implements SignatureUpdateError {

    public InvalidProxySettingsException() { }

    @Override
    public String getMessage(Context context) {
        return context.getString(R.string.main_settings_proxy_invalid_settings);
    }
}
