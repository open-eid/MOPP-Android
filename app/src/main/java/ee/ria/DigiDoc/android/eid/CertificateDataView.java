package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.idcard.CertificateType;
import io.reactivex.rxjava3.core.Observable;

import static com.jakewharton.rxbinding4.view.RxView.clicks;

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

    public void data(CertificateType type, Certificate data, int pinRetryCount, int pukRetryCount) {
        boolean pinBlocked = pinRetryCount == 0;
        boolean pukBlocked = pukRetryCount == 0;
        buttonUnblocks = pinBlocked && !pukBlocked;

        titleView.setText(type == CertificateType.AUTHENTICATION
                ? R.string.eid_home_certificate_data_title_auth
                : R.string.eid_home_certificate_data_title_sign);
        validityView.setText(formatter.certificateDataValidity(type, data));
        if (!data.expired()) {
            validityView.setContentDescription(getResources().getString(R.string.eid_home_certificate_data_valid_accessibility) + formatter.instantAccessibility(data.notAfter(), false));
        }
        int buttonChange = type == CertificateType.AUTHENTICATION
                ? R.string.eid_home_certificate_data_button_change_auth
                : R.string.eid_home_certificate_data_button_change_sign;
        int buttonUnblock = type == CertificateType.AUTHENTICATION
                ? R.string.eid_home_certificate_data_button_unblock_auth
                : R.string.eid_home_certificate_data_button_unblock_sign;
        String buttonChangeAccessibility = type == CertificateType.AUTHENTICATION
                ? (getResources().getString(R.string.eid_home_certificate_data_button_change_auth_accessibility)).toLowerCase()
                : (getResources().getString(R.string.eid_home_certificate_data_button_change_sign_accessibility)).toLowerCase();
        String buttonUnblockAccessibility = type == CertificateType.AUTHENTICATION
                ? (getResources().getString(R.string.eid_home_certificate_data_button_unblock_auth_accessibility)).toLowerCase()
                : (getResources().getString(R.string.eid_home_certificate_data_button_unblock_sign_accessibility)).toLowerCase();
        buttonView.setText(buttonUnblocks ? buttonUnblock : buttonChange);
        buttonView.setContentDescription(buttonUnblocks ? buttonUnblockAccessibility : buttonChangeAccessibility);
        linkView.setText(type == CertificateType.AUTHENTICATION
                ? R.string.eid_home_certificate_data_link_auth
                : R.string.eid_home_certificate_data_link_sign);
        linkView.setContentDescription(type == CertificateType.AUTHENTICATION
                ? getContext().getString(R.string.eid_home_certificate_data_link_auth_accessibility)
                : getContext().getString(R.string.eid_home_certificate_data_link_sign_accessibility));
        errorView.setText(type == CertificateType.AUTHENTICATION
                ? R.string.eid_home_certificate_data_error_auth
                : R.string.eid_home_certificate_data_error_sign);
        errorView.setContentDescription(type == CertificateType.AUTHENTICATION
                ? getContext().getString(R.string.eid_home_certificate_data_error_auth_accessibility)
                : getContext().getString(R.string.eid_home_certificate_data_error_sign_accessibility));

        if (!pinBlocked && !pukBlocked) {
            buttonView.setVisibility(VISIBLE);
            linkView.setVisibility(VISIBLE);
            errorView.setVisibility(GONE);
        } else if (pinBlocked && pukBlocked) {
            buttonView.setVisibility(GONE);
            linkView.setVisibility(GONE);
            errorView.setVisibility(VISIBLE);
            errorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        } else if (!pinBlocked) {
            buttonView.setVisibility(VISIBLE);
            linkView.setVisibility(GONE);
            errorView.setVisibility(GONE);
        } else {
            buttonView.setVisibility(VISIBLE);
            linkView.setVisibility(GONE);
            errorView.setVisibility(VISIBLE);
            errorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
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
