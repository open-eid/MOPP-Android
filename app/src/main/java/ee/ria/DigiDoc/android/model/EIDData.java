package ee.ria.DigiDoc.android.model;

import android.support.annotation.Nullable;

import org.threeten.bp.LocalDate;

import ee.ria.DigiDoc.EIDType;

public interface EIDData {

    @Nullable EIDType type();

    String givenNames();

    String surname();

    String personalCode();

    String citizenship();

    @Nullable LocalDate dateOfBirth();

    CertificateData authCertificate();

    CertificateData signCertificate();

    int pukRetryCount();
}
