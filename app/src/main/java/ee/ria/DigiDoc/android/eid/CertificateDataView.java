package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.tokenlibrary.Token;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class CertificateDataView extends LinearLayout {

    private final Formatter formatter;

    private final TextView titleView;
    private final TextView validityView;
    private final Button buttonView;
    private final TextView linkView;
    private final TextView errorView;

    private boolean buttonUnblocks = false;

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
        formatter = Application.component(context).formatter();
        setOrientation(VERTICAL);
        inflate(context, R.layout.eid_home_certificate_data, this);
        titleView = findViewById(R.id.eidHomeCertificateDataTitle);
        validityView = findViewById(R.id.eidHomeCertificateDataValidity);
        buttonView = findViewById(R.id.eidHomeCertificateDataButton);
        linkView = findViewById(R.id.eidHomeCertificateDataLink);
        errorView = findViewById(R.id.eidHomeCertificateDataError);

        formatter.underline(linkView);
    }

    public void data(Token.CertType type, CertificateData data, int pukRetryCount) {
        boolean pinBlocked = data.pinRetryCount() == 0;
        boolean pukBlocked = pukRetryCount == 0;
        buttonUnblocks = pinBlocked && !pukBlocked;

        titleView.setText(type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_title_auth
                : R.string.eid_home_certificate_data_title_sign);
        validityView.setText(formatter.certificateDataValidity(type, data));
        int buttonChange = type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_button_change_auth
                : R.string.eid_home_certificate_data_button_change_sign;
        int buttonUnblock = type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_button_unblock_auth
                : R.string.eid_home_certificate_data_button_unblock_sign;
        buttonView.setText(buttonUnblocks ? buttonUnblock : buttonChange);
        linkView.setText(type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_link_auth
                : R.string.eid_home_certificate_data_link_sign);
        errorView.setText(type == Token.CertType.CertAuth
                ? R.string.eid_home_certificate_data_error_auth
                : R.string.eid_home_certificate_data_error_sign);

        if (!pinBlocked && !pukBlocked) {
            buttonView.setVisibility(VISIBLE);
            linkView.setVisibility(VISIBLE);
            errorView.setVisibility(GONE);
        } else if (pinBlocked && pukBlocked) {
            buttonView.setVisibility(GONE);
            linkView.setVisibility(GONE);
            errorView.setVisibility(VISIBLE);
        } else if (!pinBlocked) {
            buttonView.setVisibility(VISIBLE);
            linkView.setVisibility(GONE);
            errorView.setVisibility(GONE);
        } else {
            buttonView.setVisibility(VISIBLE);
            linkView.setVisibility(GONE);
            errorView.setVisibility(VISIBLE);
        }
    }

    public Observable<String> updateTypes() {
        return Observable.merge(
                clicks(buttonView)
                        .map(ignored ->
                                buttonUnblocks ? CodeUpdateType.UNBLOCK : CodeUpdateType.EDIT),
                clicks(linkView).map(ignored -> CodeUpdateType.UNBLOCK));
    }
}
