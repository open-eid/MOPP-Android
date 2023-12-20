package ee.ria.DigiDoc.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptedPreferences {

    private static final String ENCRYPTED_PREFERENCES_KEY = "encryptedPreferencesStorage";

    public static SharedPreferences getEncryptedPreferences(Context context) throws IOException, GeneralSecurityException {
        String masterKey;
        masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        return EncryptedSharedPreferences.create(
                ENCRYPTED_PREFERENCES_KEY,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
