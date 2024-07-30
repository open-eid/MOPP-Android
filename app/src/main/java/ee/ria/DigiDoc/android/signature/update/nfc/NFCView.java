package ee.ria.DigiDoc.android.signature.update.nfc;

import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;
import static ee.ria.DigiDoc.android.Constants.CAN_LENGTH;
import static ee.ria.DigiDoc.android.accessibility.AccessibilityUtils.removeAccessibilityStateChanged;
import static ee.ria.DigiDoc.common.PinConstants.PIN2_MIN_LENGTH;
import static ee.ria.DigiDoc.common.PinConstants.PIN_MAX_LENGTH;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.nio.charset.StandardCharsets;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.Constants;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.ErrorMessageUtil;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.PinConstants;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

public class NFCView extends LinearLayout implements SignatureAddView<NFCRequest, NFCResponse> {
    private final Navigator navigator;

    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final LinearLayout nfcFoundLayout;
    private final LinearLayout nfcNotFoundLayout;
    private final TextView message;
    private final EditText canView;
    private final TextInputLayout canLayout;
    private final MaterialTextView canLabel;
    private final EditText pinView;
    private final TextInputLayout pinLayout;
    private final MaterialTextView pinLabel;

    private AccessibilityManager.TouchExplorationStateChangeListener accessibilityTouchExplorationStateChangeListener;

    public NFCView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_nfc, this);

        navigator = ApplicationApp.component(context).navigator();
        nfcFoundLayout = findViewById(R.id.signatureUpdateNFCFoundLayout);
        nfcNotFoundLayout = findViewById(R.id.signatureUpdateNFCNotFoundLayout);
        message = findViewById(R.id.signatureUpdateNFCMessage);
        canView = findViewById(R.id.signatureUpdateNFCCAN);
        canLayout = findViewById(R.id.signatureUpdateNFCCANLayout);
        canLabel = findViewById(R.id.signatureUpdateNFCCANLabel);
        pinView = findViewById(R.id.signatureUpdateNFCPIN2);
        pinLayout = findViewById(R.id.signatureUpdateNFCPIN2Layout);
        pinLabel = findViewById(R.id.signatureUpdateNFCPIN2Label);

        handleNFCSupportLayout();

        if (AccessibilityUtils.isTalkBackEnabled()) {
            AccessibilityUtils.setSingleCharactersContentDescription(canView, "Card number");
            AccessibilityUtils.setSingleCharactersContentDescription(pinView, "PIN code");
            AccessibilityUtils.setEditTextCursorToEnd(canView);
            AccessibilityUtils.setEditTextCursorToEnd(pinView);
            AccessibilityUtils.setTextViewContentDescription(context, true, null, canLabel.getText().toString(), canView);
            AccessibilityUtils.setTextViewContentDescription(context, true, null, pinLabel.getText().toString(), pinView);
        }
        checkInputsValidity();
    }

    public NFCView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NFCView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public NFCView(Context context) {
        this(context, null, 0, 0);
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(canView) , afterTextChangeEvents(pinView));
    }

    public boolean positiveButtonEnabled() {
        Editable canText = canView.getText();
        Editable pinText = pinView.getText();
        return canText != null && isCANLengthValid(canText.toString()) && pinText != null && isPinLengthValid(pinText.toString());
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        canView.setText(viewModel.can());
        pinView.setText("");
        AccessibilityUtils.setEditTextCursorToEnd(canView);
        AccessibilityUtils.setEditTextCursorToEnd(pinView);
        ErrorMessageUtil.setTextViewError(getContext(), null, canLabel, canLayout, canView);
        ErrorMessageUtil.setTextViewError(getContext(), null, pinLabel, pinLayout, pinView);
        message.clearFocus();
        canView.clearFocus();
        pinView.clearFocus();
    }

    @Override
    public NFCRequest request() {
        NFCRequest nfcRequest = NFCRequest.create(canView.getText().toString(),
                pinView.getText().toString().getBytes(StandardCharsets.UTF_8));
        pinView.setText(null);

        return nfcRequest;
    }

    @Override
    public void response(@Nullable NFCResponse response, @Nullable RadioGroup methodView) {
        positiveButtonStateSubject.onNext(Constants.VOID);

        if (methodView != null) {
            handleAccessibility(methodView);
        }
    }

    private void checkInputsValidity() {
        canView.setOnFocusChangeListener((view, hasfocus) -> checkCANCodeValidity());
        pinView.setOnFocusChangeListener((view, hasfocus) -> checkPinCodeValidity());
    }

    private void checkCANCodeValidity() {
        canLayout.setError(null);
        Editable canCodeView = canView.getText();
        if (canCodeView != null && !canCodeView.toString().isEmpty() &&
                !isCANLengthValid(canCodeView.toString())
        ) {
            canLayout.setError(getResources().getString(
                    R.string.nfc_sign_can_invalid_length,
                    Integer.toString(CAN_LENGTH)));
        }
    }

    private void checkPinCodeValidity() {
        pinLayout.setError(null);
        Editable pinCodeView = pinView.getText();
        if (pinCodeView != null && !pinCodeView.toString().isEmpty() &&
                !isPinLengthValid(pinCodeView.toString())
        ) {
            pinLayout.setError(getResources().getString(
                    R.string.id_card_sign_pin_invalid_length,
                    getResources().getString(R.string.signature_id_card_pin2),
                    Integer.toString(PIN2_MIN_LENGTH),
                    Integer.toString(PIN_MAX_LENGTH)));
        }
    }

    private boolean isPinLengthValid(String pin) {
        return pin.length() >= PinConstants.PIN2_MIN_LENGTH &&
                pin.length() <= PinConstants.PIN_MAX_LENGTH;
    }

    private boolean isCANLengthValid(String can) {
        return can.length() == CAN_LENGTH;
    }

    private void setAccessibilityDescription() {
        canView.setContentDescription(getResources().getString(R.string.signature_update_nfc_can) + " " +
                AccessibilityUtils.getTextViewAccessibility(canView));
        AccessibilityUtils.setSingleCharactersContentDescription(canView,
                getResources().getString(R.string.signature_update_nfc_can));
        AccessibilityUtils.setEditTextCursorToEnd(canView);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Better support for Voice Assist to not delete wrong characters
        accessibilityTouchExplorationStateChangeListener = AccessibilityUtils.addAccessibilityStateChanged(enabled -> {
            boolean isTalkBackEnabled = AccessibilityUtils.isTalkBackEnabled();
            if (isTalkBackEnabled) {
                setAccessibilityDescription();
            } else {
                AccessibilityUtils.setJoinedCharactersContentDescription(canView);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        removeAccessibilityStateChanged(accessibilityTouchExplorationStateChangeListener);
    }

    private boolean isNFCSupported() {
        NfcManager manager = (NfcManager) navigator.activity().getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Timber.log(Log.ERROR, "NFC is not supported on this device");
            return false;
        }

        return true;
    }

    private void handleNFCSupportLayout() {
        if (isNFCSupported()) {
            nfcNotFoundLayout.setVisibility(GONE);
            nfcFoundLayout.setVisibility(VISIBLE);
        } else {
            nfcFoundLayout.setVisibility(GONE);
            nfcNotFoundLayout.setVisibility(VISIBLE);
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
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info) {
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

        RadioButton idCardRadioButton = methodView.findViewById(R.id.signatureUpdateSignatureAddMethodIdCard);
        CharSequence idCardContentDescription = idCardRadioButton.getContentDescription();
        idCardRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        idCardRadioButton.setContentDescription("");
        setupContentDescriptions(idCardRadioButton, idCardContentDescription);

        postDelayed(() -> {
            mobileIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            mobileIdRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_mobile_id, 1, 4));

            smartIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            smartIdRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_smart_id, 2, 4));

            idCardRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            idCardRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_id_card, 3, 4));
        }, 3500);
    }
}
