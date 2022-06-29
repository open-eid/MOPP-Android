package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import net.cachapa.expandablelayout.ExpandableLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.common.TextUtil;
import ee.ria.DigiDoc.idcard.CertificateType;
import ee.ria.DigiDoc.idcard.CodeType;
import io.reactivex.rxjava3.core.Observable;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;

public final class EIDDataView extends LinearLayout {

    private final Formatter formatter;

    private final TextView typeView;
    private final TextView givenNamesView;
    private final TextView surnameView;
    private final TextView personalCodeLabelView;
    private final TextView personalCodeView;
    private final TextView citizenshipView;
    private final View documentNumberLabelView;
    private final TextView documentNumberView;
    private final View expiryDateLabelView;
    private final TextView expiryDateView;
    private final TextView certificatesTitleView;
    private final ExpandableLayout certificatesContainerView;
    private final CertificateDataView authCertificateDataView;
    private final CertificateDataView signCertificateDataView;
    private final View pukButtonView;
    private final View pukErrorView;
    private final TextView pukLinkView;

    @ColorInt private final int collapsedTitleColor;
    @ColorInt private final int expandedTitleColor;

    public EIDDataView(Context context) {
        this(context, null);
    }

    public EIDDataView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EIDDataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EIDDataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                       int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        formatter = Application.component(context).formatter();
        setOrientation(VERTICAL);
        inflate(context, R.layout.eid_home_data, this);
        typeView = findViewById(R.id.eidHomeDataType);
        givenNamesView = findViewById(R.id.eidHomeDataGivenNames);
        surnameView = findViewById(R.id.eidHomeDataSurname);
        personalCodeLabelView = findViewById(R.id.eidHomeDataPersonalCodeLabel);
        personalCodeView = findViewById(R.id.eidHomeDataPersonalCode);
        citizenshipView = findViewById(R.id.eidHomeDataCitizenship);
        documentNumberLabelView = findViewById(R.id.eidHomeDataDocumentNumberLabel);
        documentNumberView = findViewById(R.id.eidHomeDataDocumentNumber);
        expiryDateLabelView = findViewById(R.id.eidHomeDataExpiryDateLabel);
        expiryDateView = findViewById(R.id.eidHomeDataExpiryDate);
        certificatesTitleView = findViewById(R.id.eidHomeDataCertificatesTitle);
        certificatesContainerView = findViewById(R.id.eidHomeDataCertificatesContainer);
        authCertificateDataView = findViewById(R.id.eidHomeDataCertificatesAuth);
        signCertificateDataView = findViewById(R.id.eidHomeDataCertificatesSign);
        pukButtonView = findViewById(R.id.eidHomeDataCertificatesPukButton);
        pukErrorView = findViewById(R.id.eidHomeDataCertificatesPukError);
        pukLinkView = findViewById(R.id.eidHomeDataCertificatesPukLink);

        tintCompoundDrawables(certificatesTitleView);
        formatter.underline(pukLinkView);

        TypedArray a = getContext().obtainStyledAttributes(new int[] {
                android.R.attr.textColorSecondary, R.attr.colorAccent});
        collapsedTitleColor = a.getColor(0, Color.BLACK);
        expandedTitleColor = a.getColor(1, Color.BLACK);
        a.recycle();
    }

    public void render(@NonNull IdCardData data, boolean certificateContainerExpanded) {
        typeView.setText(formatter.eidType(data.type()));
        typeView.setContentDescription(formatter.eidType(data.type()).toString().toLowerCase());
        givenNamesView.setText(data.personalData().givenNames());
        givenNamesView.setContentDescription(data.personalData().givenNames().toLowerCase());
        surnameView.setText(data.personalData().surname());
        surnameView.setContentDescription(data.personalData().surname().toLowerCase());
        personalCodeLabelView.setContentDescription(personalCodeLabelView.getText().toString().toLowerCase());
        personalCodeView.setText(data.personalData().personalCode());
        personalCodeView.setContentDescription(TextUtil.splitTextAndJoin(data.personalData().personalCode().toLowerCase(), "", " "));
        citizenshipView.setText(data.personalData().citizenship());
        citizenshipView.setContentDescription(TextUtil.splitTextAndJoin(data.personalData().citizenship().toLowerCase(), "", " "));

        certificatesTitleView.setTextColor(certificateContainerExpanded
                ? expandedTitleColor
                : collapsedTitleColor);
        int drawable = certificateContainerExpanded
                ? R.drawable.ic_icon_accordion_expanded
                : R.drawable.ic_icon_accordion_collapsed;
        certificatesTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0);
        tintCompoundDrawables(certificatesTitleView);
        setCustomClickAccessibilityFeedBack(certificatesTitleView);

        certificatesContainerView.setExpanded(certificateContainerExpanded);

        authCertificateDataView.data(CertificateType.AUTHENTICATION, data.authCertificate(),
                data.pin1RetryCount(), data.pukRetryCount());
        signCertificateDataView.data(CertificateType.SIGNING, data.signCertificate(),
                data.pin2RetryCount(), data.pukRetryCount());
        if (data.authCertificate().expired() && data.signCertificate().expired()) {
            pukButtonView.setVisibility(GONE);
            pukErrorView.setVisibility(GONE);
            pukLinkView.setVisibility(GONE);
        } else if (data.pukRetryCount() == 0) {
            pukButtonView.setVisibility(GONE);
            pukErrorView.setVisibility(VISIBLE);
            pukLinkView.setVisibility(VISIBLE);
            pukErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        } else {
            pukButtonView.setVisibility(VISIBLE);
            pukErrorView.setVisibility(GONE);
            pukLinkView.setVisibility(GONE);
        }
        documentNumberView.setText(data.personalData().documentNumber());
        documentNumberView.setContentDescription(TextUtil.splitTextAndJoin(data.personalData().documentNumber(), "", " "));
        expiryDateView.setText(formatter.idCardExpiryDate(data.personalData().expiryDate()));
        documentNumberLabelView.setVisibility(VISIBLE);
        documentNumberView.setVisibility(VISIBLE);
        expiryDateLabelView.setVisibility(VISIBLE);
        expiryDateView.setVisibility(VISIBLE);
    }

    public Observable<Boolean> certificateContainerStates() {
        return clicks(certificatesTitleView)
                .map(ignored -> !certificatesContainerView.isExpanded());
    }

    @SuppressWarnings("unchecked")
    public Observable<CodeUpdateAction> actions() {
        return Observable.mergeArray(
                authCertificateDataView.updateTypes()
                        .map(updateType -> CodeUpdateAction.create(CodeType.PIN1, updateType)),
                signCertificateDataView.updateTypes()
                        .map(updateType -> CodeUpdateAction.create(CodeType.PIN2, updateType)),
                clicks(pukButtonView)
                        .map(ignored -> CodeUpdateAction.create(CodeType.PUK, CodeUpdateType.EDIT)),
                clicks(pukLinkView)
                        .map(ignored ->
                                CodeUpdateAction.create(CodeType.PUK, CodeUpdateType.UNBLOCK))
        );
    }

    private void setCustomClickAccessibilityFeedBack(TextView certificatesTitleView) {
        ViewCompat.setAccessibilityDelegate(certificatesTitleView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String message;
                if (certificatesContainerView.isExpanded()) {
                    message = "deactivate";
                } else {
                    message = "activate";
                }
                AccessibilityNodeInfoCompat.AccessibilityActionCompat customClick = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK, message);
                info.addAction(customClick);
            }
        });
    }
}
