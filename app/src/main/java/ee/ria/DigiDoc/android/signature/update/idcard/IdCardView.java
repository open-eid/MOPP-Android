package ee.ria.DigiDoc.android.signature.update.idcard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardSignResponse;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.idcard.Token;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderStatus;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;
import static ee.ria.DigiDoc.android.Constants.VOID;

public final class IdCardView extends LinearLayout implements
        SignatureAddView<IdCardRequest, IdCardResponse> {

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View signContainerView;
    private final View signContainerMessage;
    private final TextView signDataView;
    private final EditText signPin2View;
    private final TextView signPin2ErrorView;

    @Nullable private Token token;

    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();

    public IdCardView(Context context) {
        this(context, null);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                      int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_id_card, this);
        progressContainerView = findViewById(R.id.signatureUpdateIdCardProgressContainer);
        progressMessageView = findViewById(R.id.signatureUpdateIdCardProgressMessage);
        signContainerView = findViewById(R.id.signatureUpdateIdCardSignContainer);
        signContainerMessage = findViewById(R.id.signatureUpdateIdCardSignMessage);
        signDataView = findViewById(R.id.signatureUpdateIdCardSignData);
        signPin2View = findViewById(R.id.signatureUpdateIdCardSignPin2);
        signPin2ErrorView = findViewById(R.id.signatureUpdateIdCardSignPin2Error);
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(signPin2View));
    }

    public boolean positiveButtonEnabled() {
        return token != null && signPin2View.getText().length() >= 4;
    }

    @Override
    public void reset(@Nullable SignatureUpdateViewModel viewModel) {
        signPin2View.setText(null);
        progressMessageView.setContentDescription(null);
    }

    @Override
    public IdCardRequest request() {
        return IdCardRequest.create(token, signPin2View.getText().toString());
    }

    @Override
    public void response(@Nullable IdCardResponse response, RadioGroup methodView) {
        IdCardDataResponse dataResponse = response == null ? null : response.dataResponse();
        IdCardSignResponse signResponse = response == null ? null : response.signResponse();

        if (signResponse != null && signResponse.state().equals(State.CLEAR)) {
            reset(null);
        }

        IdCardData data = dataResponse == null ? null : dataResponse.data();
        if (data == null && signResponse != null) {
            data = signResponse.data();
        }

        if (methodView != null) {
            handleAccessibility(methodView);
        }

        token = dataResponse == null ? null : dataResponse.token();
        if (token == null && signResponse != null) {
            token = signResponse.token();
        }
        positiveButtonStateSubject.onNext(VOID);

        if (signResponse != null && signResponse.state().equals(State.ACTIVE)) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_signing);
            progressMessageView.setContentDescription(
                    getResources().getString(R.string.signature_update_id_card_progress_message_signing));
            progressMessageView.postDelayed(() -> {
                progressMessageView.requestFocus();
                progressMessageView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), R.string.signature_update_id_card_progress_message_signing);
            }, 1000);

            signContainerView.setVisibility(GONE);
        } else if (signResponse != null && signResponse.error() != null && data != null) {
            int pinRetryCount = data.pin2RetryCount();
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.personalData().givenNames(),
                    data.personalData().surname(), data.personalData().personalCode()));
            signPin2ErrorView.setVisibility(VISIBLE);
            signPin2ErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            if (pinRetryCount == 1) {
                signPin2ErrorView.setText(
                        R.string.signature_update_id_card_sign_pin2_invalid_final);
            } else {
                signPin2ErrorView.setText(getResources().getString(
                        R.string.signature_update_id_card_sign_pin2_invalid, pinRetryCount));
                signPin2ErrorView.setContentDescription(getResources().getString(
                        R.string.signature_update_id_card_sign_pin2_invalid, pinRetryCount));
            }
        } else if (dataResponse != null && data != null) {
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.personalData().givenNames(),
                    data.personalData().surname(), data.personalData().personalCode()));
            signPin2ErrorView.setVisibility(GONE);
        } else if (dataResponse != null
                && dataResponse.status().equals(SmartCardReaderStatus.CARD_DETECTED)) {

            progressMessageView.setText(R.string.signature_update_id_card_progress_message_card_detected);

            progressMessageView.postDelayed(() -> {
                progressMessageView.requestFocus();
                progressMessageView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), R.string.signature_update_id_card_progress_message_card_detected);
            }, 700);
            signContainerView.setVisibility(GONE);

        } else if (dataResponse != null
                && dataResponse.status().equals(SmartCardReaderStatus.READER_DETECTED)) {

            progressMessageView.setText(R.string.signature_update_id_card_progress_message_reader_detected);

            progressMessageView.postDelayed(() -> {
                progressMessageView.requestFocus();
                progressMessageView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), R.string.signature_update_id_card_progress_message_reader_detected);
            }, 700);
        } else {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_initial);
            progressMessageView.setContentDescription(
                    getResources().getString(R.string.signature_update_id_card_progress_message_initial));

            progressMessageView.postDelayed(() -> {
                progressMessageView.requestFocus();
                progressMessageView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), R.string.signature_update_id_card_progress_message_initial);
            }, 1500);

            signContainerView.setVisibility(GONE);
        }

        if (progressContainerView.getVisibility() == VISIBLE) {
            progressContainerView.postDelayed(() -> {
                progressContainerView.requestFocus();
                progressContainerView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 1000);
        }
        if (signContainerView.getVisibility() == VISIBLE) {
            signContainerView.postDelayed(() -> {
                signContainerMessage.requestFocus();
                signContainerMessage.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 100);
        }
    }

    private void setupContentDescriptions(RadioButton radioButton, CharSequence contentDescription) {
        radioButton.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                if (!event.getText().isEmpty() &&
                        (event.getText().get(0).toString().equals(
                                getResources().getString(R.string.signature_update_signature_add_method_mobile_id)) ||
                        event.getText().get(0).toString().equals(
                                getResources().getString(R.string.signature_update_signature_add_method_smart_id)))) {
                   event.getText().add(getContentDescription());
                }
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setContentDescription(contentDescription);
                info.setCheckable(false);
                info.setClickable(false);
                info.setClassName("");
                info.setPackageName("");
                info.setText(contentDescription);
                info.setViewIdResourceName("");
                info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION);
            }
        });
    }

    private void handleAccessibility(RadioGroup methodView) {
        RadioButton mobileIdRadioButton = methodView.findViewById(R.id.signatureUpdateSignatureAddMethodMobileId);
        CharSequence mobileIdContentDescription = mobileIdRadioButton.getContentDescription();
        mobileIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mobileIdRadioButton.setContentDescription("");
        setupContentDescriptions(mobileIdRadioButton, mobileIdContentDescription);

        RadioButton smartIdRadioButton = methodView.findViewById(R.id.signatureUpdateSignatureAddMethodSmartId);
        CharSequence smartIdContentDescription = smartIdRadioButton.getContentDescription();
        smartIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        smartIdRadioButton.setContentDescription("");
        setupContentDescriptions(smartIdRadioButton, smartIdContentDescription);

        postDelayed(() -> {
            mobileIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            mobileIdRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_mobile_id, 1, 3));

            smartIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            smartIdRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_smart_id, 2, 3));
        }, 3500);
    }
}
