package ee.ria.DigiDoc;

import android.support.annotation.Nullable;

public enum EIDType {

    ID_CARD, DIGI_ID, MOBILE_ID;

    @Nullable
    public static EIDType parseOrganization(@Nullable String organization) {
        if (organization != null && organization.startsWith("ESTEID")) {
            if (organization.contains("MOBIIL-ID")) {
                return MOBILE_ID;
            } else if (organization.contains("DIGI-ID")) {
                return DIGI_ID;
            } else {
                return ID_CARD;
            }
        }
        return null;
    }
}
