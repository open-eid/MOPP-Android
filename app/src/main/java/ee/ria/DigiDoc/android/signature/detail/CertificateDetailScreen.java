package ee.ria.DigiDoc.android.signature.detail;

import android.content.Context;
import android.view.View;

import java.security.cert.X509Certificate;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class CertificateDetailScreen extends ConductorScreen {

    private static final int CERTIFICATE_DETAIL_SCREEN_ID = R.id.certificateDetailScreen;
    private X509Certificate userCertificate;

    public CertificateDetailScreen(int id, X509Certificate userCertificate) {
        super(id);
        this.userCertificate = userCertificate;
    }

    public static CertificateDetailScreen create(X509Certificate certificate) {
        return new CertificateDetailScreen(CERTIFICATE_DETAIL_SCREEN_ID, certificate);
    }

    @SuppressWarnings("WeakerAccess")
    public CertificateDetailScreen() {
        super(CERTIFICATE_DETAIL_SCREEN_ID);
    }

    @Override
    protected View view(Context context) {
        return new CertificateDetailView(context, userCertificate);
    }
}
