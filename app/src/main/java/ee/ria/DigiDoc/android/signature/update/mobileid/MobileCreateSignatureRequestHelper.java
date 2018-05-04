package ee.ria.DigiDoc.android.signature.update.mobileid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ee.ria.mopp.androidmobileid.dto.request.DataFileDto;
import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;
import timber.log.Timber;

final class MobileCreateSignatureRequestHelper {

    private static final String FORMAT = "BDOC";
    private static final String VERSION = "2.1";
    private static final String MESSAGING_MODE = "asynchClientServer";
    private static final String SERVICE_NAME = "DigiDoc3";
    private static final int ASYNC_CONFIGURATION = 0;

    private static final String ESTONIAN_PHONE_CODE = "372";
    private static final String PLUS_PREFIXED_ESTONIAN_PHONE_CODE = "+" + ESTONIAN_PHONE_CODE;
    private static final String FIRST_NUMBER_IN_ESTONIAN_MOBILE_NUMBER = "5";

    private static final int MAX_DISPLAY_MESSAGE_BYTES = 40;

    private static final String DEFAULT_LANGUAGE = "ENG";
    private static final ImmutableSet<String> SUPPORTED_LANGUAGES = ImmutableSet
            .of(DEFAULT_LANGUAGE, "EST", "RUS", "LIT");

    private static final String DIGEST_TYPE = "sha256";
    private static final String DIGEST_METHOD = "http://www.w3.org/2001/04/xmlenc#sha256";

    static MobileCreateSignatureRequest create(SignedContainer container, String personalCode,
                                               String phoneNo, String displayMessage) {
        MobileCreateSignatureRequest request = new MobileCreateSignatureRequest();
        request.setFormat(FORMAT);
        request.setVersion(VERSION);
        request.setMessagingMode(MESSAGING_MODE);
        request.setServiceName(SERVICE_NAME);
        request.setAsyncConfiguration(ASYNC_CONFIGURATION);
        request.setIdCode(personalCode);
        request.setPhoneNr(addEstonianCountryCodeIfMissingAndApplicable(phoneNo));
        request.setLanguage(getLanguage());
        request.setMessageToDisplay(trimDisplayMessageIfNotWithinSizeLimit(displayMessage));
        request.setSigningProfile(signingProfile(container));
        request.setSignatureId(signatureId(container));
        request.setDatafiles(datafiles(container));
        return request;
    }

    private static String addEstonianCountryCodeIfMissingAndApplicable(String phoneNr) {
        if (!phoneNr.startsWith(ESTONIAN_PHONE_CODE)
                && !phoneNr.startsWith(PLUS_PREFIXED_ESTONIAN_PHONE_CODE)
                && phoneNr.startsWith(FIRST_NUMBER_IN_ESTONIAN_MOBILE_NUMBER)) {
            return ESTONIAN_PHONE_CODE + phoneNr;
        }
        return phoneNr;
    }

    private static String trimDisplayMessageIfNotWithinSizeLimit(String displayMessage) {
        if (displayMessage.getBytes().length > MAX_DISPLAY_MESSAGE_BYTES) {
            int bytesPerChar = displayMessage.getBytes().length / displayMessage.length();
            return displayMessage.substring(0, 36 / bytesPerChar) + "...";
        }
        return displayMessage;
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

    private static List<DataFileDto> datafiles(SignedContainer container) {
        ImmutableList<DataFile> dataFiles = container.dataFiles();
        List<DataFileDto> dtos = new ArrayList<>(dataFiles.size());
        for (DataFile dataFile : dataFiles) {
            DataFileDto dto = new DataFileDto();
            dto.setId(dataFile.id());
            dto.setDigestType(DIGEST_TYPE);
            try {
                dto.setDigestValue(container.calculateDataFileDigest(dataFile, DIGEST_METHOD));
            } catch (Exception e) {
                Timber.e(e, "Calculating digest value failed");
                dto.setDigestValue("");
            }
            dto.setMimeType(dataFile.mimeType());
            dtos.add(dto);
        }
        return dtos;
    }
}
