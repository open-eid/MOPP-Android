package ee.ria.DigiDoc.android.signature.update.nfc;

import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Constants;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.ErrorMessageUtil;
import ee.ria.DigiDoc.common.PinConstants;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class NFCView  extends LinearLayout implements SignatureAddView<NFCRequest, NFCResponse> {
    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final TextView message;
    private final EditText canView;
    private final TextInputLayout canLayout;
    private final MaterialTextView canLabel;
    private final CheckBox rememberMeView;
    private final EditText pinView;
    private final TextInputLayout pinLayout;
    private final MaterialTextView pinLabel;

    private AccessibilityManager.TouchExplorationStateChangeListener accessibilityTouchExplorationStateChangeListener;

    public NFCView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_nfc, this);

        message = findViewById(R.id.signatureUpdateNFCMessage);
        canView = findViewById(R.id.signatureUpdateNFCCAN);
        canLayout = findViewById(R.id.signatureUpdateNFCCANLayout);
        canLabel = findViewById(R.id.signatureUpdateNFCCANLabel);
        rememberMeView = findViewById(R.id.signatureUpdateNFCRememberMe);
        pinView = findViewById(R.id.signatureUpdateNFCPIN2);
        pinLayout = findViewById(R.id.signatureUpdateNFCPIN2Layout);
        pinLabel = findViewById(R.id.signatureUpdateNFCPIN2Label);
        AccessibilityUtils.setSingleCharactersContentDescription(canView, "Card number");
        AccessibilityUtils.setSingleCharactersContentDescription(pinView, "PIN code");
        AccessibilityUtils.setEditTextCursorToEnd(canView);
        AccessibilityUtils.setEditTextCursorToEnd(pinView);
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
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(pinView));
    }

    public boolean positiveButtonEnabled() {
        Editable canText = canView.getText();
        Editable pinText = pinView.getText();
        return canText != null && canText.length() == 6 && pinText != null && isPinLengthEnough(pinText.toString());
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        canView.setText(viewModel.can());
        rememberMeView.setChecked(true);
        pinView.setText("");
        AccessibilityUtils.setEditTextCursorToEnd(canView);
        AccessibilityUtils.setEditTextCursorToEnd(pinView);
        ErrorMessageUtil.setTextViewError(getContext(), null, canLabel, canLayout, canView);
        ErrorMessageUtil.setTextViewError(getContext(), null, pinLabel, pinLayout, pinView);
        message.clearFocus();
        canView.clearFocus();
        rememberMeView.clearFocus();
        pinView.clearFocus();
    }

    @Override
    public NFCRequest request() {
        return NFCRequest.create(canView.getText().toString(),
                pinView.getText().toString(), rememberMeView.isChecked());
    }

    @Override
    public void response(@Nullable NFCResponse response, @Nullable RadioGroup methodView) {
        positiveButtonStateSubject.onNext(Constants.VOID);
    }

    private void checkInputsValidity() {
        checkPinCodeValidity();
        pinView.setOnFocusChangeListener((view, hasfocus) -> checkPinCodeValidity());
    }

    private void checkPinCodeValidity() {
        pinLabel.setError(null);
        Editable pinCodeView = pinView.getText();
        if (pinCodeView != null && !pinCodeView.toString().isEmpty() &&
                !isPinLengthEnough(pinCodeView.toString())
        ) {
            pinLabel.setError(getContext().getString(R.string.id_card_sign_pin_invalid_length));
        }
    }

    private boolean isPinLengthEnough(String pin) {
        return pin.length() >= PinConstants.PIN2_MIN_LENGTH;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Better support for Voice Assist to not delete wrong characters
        //accessibilityTouchExplorationStateChangeListener = AccessibilityUtils.addAccessibilityStateChanged(enabled -> {
        //    boolean isTalkBackEnabled = AccessibilityUtils.isTalkBackEnabled();
        //    if (isTalkBackEnabled) {
        //        setAccessibilityDescription();
        //    } else {
        //        AccessibilityUtils.setJoinedCharactersContentDescription(personalCodeView);
        //    }
        //});
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //removeAccessibilityStateChanged(accessibilityTouchExplorationStateChangeListener);
    }
}
