package ee.ria.DigiDoc.android.signature.detail;

import android.content.Context;
import android.view.View;

import java.security.cert.X509Certificate;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class CertificateDetailScreen extends ConductorScreen {

    private static X509Certificate userCertificate;

    public static CertificateDetailScreen create(X509Certificate certificate) {
        userCertificate = certificate;
        return new CertificateDetailScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public CertificateDetailScreen() {
        super(R.id.certificateDetailScreen);
    }

    @Override
    protected View view(Context context) {
        return new CertificateDetailView(context, userCertificate);
    }
}
