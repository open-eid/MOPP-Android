package ee.ria.DigiDoc.android.signature.update.mobileid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ee.ria.DigiDoc.common.MessageUtil;
import ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;

final class MobileCreateSignatureRequestHelper {

    private static final String ESTONIAN_PHONE_CODE = "372";
    private static final String PLUS_PREFIXED_ESTONIAN_PHONE_CODE = "+" + ESTONIAN_PHONE_CODE;
    private static final String FIRST_NUMBER_IN_ESTONIAN_MOBILE_NUMBER = "5";

    private static final int MAX_DISPLAY_MESSAGE_BYTES = 40;

    private static final String DEFAULT_LANGUAGE = "ENG";
    private static final ImmutableSet<String> SUPPORTED_LANGUAGES = ImmutableSet
            .of(DEFAULT_LANGUAGE, "EST", "RUS", "LIT");

    private static final String DIGEST_TYPE = "SHA256";

    private static final String RELYING_PARTY_NAME = "RIA DigiDoc";
    private static final String RELYING_PARTY_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String DISPLAY_TEXT_FORMAT = "GSM-7";

    static MobileCreateSignatureRequest create(SignedContainer container, String uuid, String proxyUrl,
                                               String skUrl, String personalCode,
                                               String phoneNo, String displayMessage) {
        MobileCreateSignatureRequest request = new MobileCreateSignatureRequest();
        request.setRelyingPartyName(RELYING_PARTY_NAME);
        request.setRelyingPartyUUID(uuid == null || uuid.isEmpty() ? RELYING_PARTY_UUID : uuid);
        request.setUrl(uuid == null || uuid.isEmpty() ? proxyUrl : skUrl);
        request.setPhoneNumber("+" + phoneNo);
        request.setNationalIdentityNumber(personalCode);

        request.setContainerPath(container.file().getPath());
        request.setHashType(DIGEST_TYPE);
        request.setLanguage(getLanguage());
        request.setDisplayText(MessageUtil.trimDisplayMessageIfNotWithinSizeLimit(displayMessage, MAX_DISPLAY_MESSAGE_BYTES, 36));
        request.setDisplayTextFormat(DISPLAY_TEXT_FORMAT);
        return request;
    }

    private static String getLanguage() {
        try {
            String language = Locale.getDefault().getISO3Language().toUpperCase();
            return SUPPORTED_LANGUAGES.contains(language) ? language : DEFAULT_LANGUAGE;
        } catch (Exception e) {
            return DEFAULT_LANGUAGE;
        }
    }

    private static String signingProfile(SignedContainer container) {
        if ("time-stamp".equals(container.signatureProfile())) {
            return "LT";
        } else {
            return "LT_TM";
        }
    }

    private static String signatureId(SignedContainer container) {
        ImmutableList<Signature> signatures = container.signatures();
        List<String> ids = new ArrayList<>(signatures.size());
        for (Signature signature : signatures) {
            ids.add(signature.id().toUpperCase());
        }

        int id = 0;
        while (ids.contains("S" + id)) {
            id++;
        }
        return "S" + id;
    }
}
