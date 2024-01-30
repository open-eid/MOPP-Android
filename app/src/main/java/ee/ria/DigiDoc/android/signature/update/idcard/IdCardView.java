package ee.ria.DigiDoc.android.signature.update.idcard;

import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;
import static ee.ria.DigiDoc.android.Constants.VOID;
import static ee.ria.DigiDoc.common.PinConstants.PIN2_MIN_LENGTH;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

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

public final class IdCardView extends LinearLayout implements
        SignatureAddView<IdCardRequest, IdCardResponse> {

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View signContainerView;
    private final View signContainerMessage;
    private final TextView signDataView;
    private final TextInputLayout signPin2Label;
    private final TextInputEditText signPin2View;
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
        signPin2Label = findViewById(R.id.signatureUpdateIdCardSignPin2Label);
        signPin2View = findViewById(R.id.signatureUpdateIdCardSignPin2);
        signPin2ErrorView = findViewById(R.id.signatureUpdateIdCardSignPin2Error);

        checkForDoneButtonClick();
        checkInputsValidity();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            CharSequence errorText = signPin2Label.getError();
            if (errorText != null) {
                SpannableStringBuilder spannable = new SpannableStringBuilder(errorText);
                spannable.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, errorText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                signPin2Label.setError(spannable);
            }
        } else {
            CharSequence errorText = signPin2Label.getError();
            if (errorText != null) {
                SpannableStringBuilder spannable = new SpannableStringBuilder(errorText);
                spannable.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), 0, errorText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                signPin2Label.setError(spannable);
            }
        }
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(signPin2View));
    }

    public boolean positiveButtonEnabled() {
        Editable pinCodeText = signPin2View.getText();
        return token != null && pinCodeText != null && isPinLengthEnough(pinCodeText.toString());
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
            signDataView.setContentDescription(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.personalData().givenNames(),
                    data.personalData().surname(), AccessibilityUtils.getTextAsSingleCharacters(data.personalData().personalCode())));
            signContainerMessage.setContentDescription(
                    getResources().getString(R.string.signature_update_id_card_sign_message) + ". " +
                            signDataView.getContentDescription().toString().toLowerCase() + ". " +
                            getResources().getString(R.string.signature_update_id_card_sign_pin2)
            );
            signPin2ErrorView.setVisibility(VISIBLE);
            signDataView.clearFocus();
            progressMessageView.clearFocus();
            if (pinRetryCount == 1) {
                signPin2ErrorView.setText(
                        R.string.signature_update_id_card_sign_pin2_invalid_final);
                signPin2ErrorView.setContentDescription(
                        getResources().getString(R.string.signature_update_id_card_sign_pin2_invalid_final_accessibility)
                );
            } else {
                signPin2ErrorView.setText(getResources().getString(
                        R.string.signature_update_id_card_sign_pin2_invalid, pinRetryCount));
                signPin2ErrorView.setContentDescription(getResources().getString(
                        R.string.signature_update_id_card_sign_pin2_invalid_accessibility, pinRetryCount));
            }
            signPin2ErrorView.postDelayed(() -> {
                signPin2ErrorView.requestFocus();
                signPin2ErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 700);
        } else if (dataResponse != null && data != null) {
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.personalData().givenNames(),
                    data.personalData().surname(), data.personalData().personalCode()));
            signDataView.setContentDescription(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.personalData().givenNames(),
                    data.personalData().surname(), AccessibilityUtils.getTextAsSingleCharacters(data.personalData().personalCode())));
            signContainerMessage.setContentDescription(
                    getResources().getString(R.string.signature_update_id_card_sign_message) + ". " +
                            signDataView.getContentDescription().toString().toLowerCase() + ". " +
                            getResources().getString(R.string.signature_update_id_card_sign_pin2)
            );
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

    private void checkInputsValidity() {
        checkPinCodeValidity();

        signPin2View.setOnFocusChangeListener((view, hasfocus) -> checkPinCodeValidity());
    }

    private void checkPinCodeValidity() {
        signPin2Label.setError(null);

        Editable pinCodeView = signPin2View.getText();

        if (pinCodeView != null && !pinCodeView.toString().isEmpty() &&
                !isPinLengthEnough(pinCodeView.toString())) {
            signPin2Label.setError(getResources().getString(
                    R.string.id_card_sign_pin_invalid_length,
                    getResources().getString(R.string.signature_id_card_pin2),
                    Integer.toString(PIN2_MIN_LENGTH)));
        }
    }

    private boolean isPinLengthEnough(String pin) {
        return pin.length() >= PIN2_MIN_LENGTH;
    }

    private void checkForDoneButtonClick() {
        // Remove focus on "Done" click
        signPin2View.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                signPin2View.setEnabled(false);
                signPin2View.setEnabled(true);
            }
            return false;
        });
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

        progressMessageView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                List<AccessibilityNodeInfo.AccessibilityAction> actionList = new ArrayList<>(info.getActionList());

                for (AccessibilityNodeInfo.AccessibilityAction action : actionList) {
                    info.removeAction(action);
                }
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
