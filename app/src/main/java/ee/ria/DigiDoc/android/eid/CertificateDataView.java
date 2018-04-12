package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.tokenlibrary.Token;

public final class CertificateDataView extends LinearLayout {

    private final TextView titleView;
    private final TextView validityView;
    private final Button buttonView;
    private final TextView linkView;
    private final TextView errorView;

    public CertificateDataView(Context context) {
        this(context, null);
    }

    public CertificateDataView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CertificateDataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CertificateDataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                               int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.eid_home_certificate_data, this);
        titleView = findViewById(R.id.eidHomeCertificateDataTitle);
        validityView = findViewById(R.id.eidHomeCertificateDataValidity);
        buttonView = findViewById(R.id.eidHomeCertificateDataButton);
        linkView = findViewById(R.id.eidHomeCertificateDataLink);
        errorView = findViewById(R.id.eidHomeCertificateDataError);
    }

    public void data(Token.CertType type, CertificateData data, int pukRetryCount) {
        titleView.setText(type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_title_auth
                : R.string.eid_home_certificate_data_title_sign);
        linkView.setText(type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_link_auth
                : R.string.eid_home_certificate_data_link_sign);
        errorView.setText(type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_error_auth
                : R.string.eid_home_certificate_data_error_sign);
    }
}
