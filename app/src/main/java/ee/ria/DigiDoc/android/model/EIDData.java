package ee.ria.DigiDoc.android.model;

import android.support.annotation.Nullable;

import org.threeten.bp.LocalDate;

public interface EIDData {

    @Nullable @EIDType String type();

    String givenNames();

    String surname();

    String personalCode();

    String citizenship();

    @Nullable LocalDate dateOfBirth();

    CertificateData authCertificate();

    CertificateData signCertificate();

    int pukRetryCount();
}
