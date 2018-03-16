package ee.ria.DigiDoc.android.signature.update;

import com.google.auto.value.AutoValue;

abstract class SignatureAddData {

    static MobileIdSignatureAddData mobileId(String phoneNo, String personalCode,
                                                    boolean rememberMe) {
        return MobileIdSignatureAddData.create(phoneNo, personalCode, rememberMe);
    }

    static IdCardSignatureAddData idCard() {
        return IdCardSignatureAddData.create();
    }

    @AutoValue
    abstract static class MobileIdSignatureAddData extends SignatureAddData {

        abstract String phoneNo();

        abstract String personalCode();

        abstract boolean rememberMe();

        private static MobileIdSignatureAddData create(String phoneNo, String personalCode,
                                               boolean rememberMe) {
            return new AutoValue_SignatureAddData_MobileIdSignatureAddData(phoneNo, personalCode,
                    rememberMe);
        }
    }

    @AutoValue
    abstract static class IdCardSignatureAddData extends SignatureAddData {

        private static IdCardSignatureAddData create() {
            return new AutoValue_SignatureAddData_IdCardSignatureAddData();
        }
    }
}
