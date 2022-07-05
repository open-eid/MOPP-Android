package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardResponse;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardView;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdView;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdResponse;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdView;
import ee.ria.DigiDoc.android.utils.TextUtil;
import io.reactivex.rxjava3.core.Observable;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static com.jakewharton.rxbinding4.widget.RxRadioGroup.checkedChanges;

public final class SignatureUpdateSignatureAddView extends LinearLayout {

    private final RadioGroup methodView;
    private final MobileIdView mobileIdView;
    private final SmartIdView smartIdView;
    private final IdCardView idCardView;

    private final Observable<Integer> methodChanges;

    public SignatureUpdateSignatureAddView(Context context) {
        this(context, null);
    }

    public SignatureUpdateSignatureAddView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureUpdateSignatureAddView(Context context, @Nullable AttributeSet attrs,
                                           int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SignatureUpdateSignatureAddView(Context context, @Nullable AttributeSet attrs,
                                           int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_signature_add, this);
        methodView = findViewById(R.id.signatureUpdateSignatureAddMethod);
        mobileIdView = findViewById(R.id.signatureUpdateMobileId);
        smartIdView = findViewById(R.id.signatureUpdateSmartId);
        idCardView = findViewById(R.id.signatureUpdateIdCard);
        methodChanges = checkedChanges(methodView).skipInitialValue().publish().autoConnect();

        RadioButton mobileIdRadioButton = findViewById(R.id.signatureUpdateSignatureAddMethodMobileId);
        RadioButton smartIdRadioButton = findViewById(R.id.signatureUpdateSignatureAddMethodSmartId);
        RadioButton idCardIdRadioButton = findViewById(R.id.signatureUpdateSignatureAddMethodIdCard);


        setupContentDescriptions(mobileIdRadioButton,
                getResources().getString(R.string.signature_update_signature_selected_method_mobile_id, 1, 3));
        setupContentDescriptions(smartIdRadioButton,
                getResources().getString(R.string.signature_update_signature_selected_method_smart_id, 2, 3));
        setupContentDescriptions(idCardIdRadioButton,
                getResources().getString(R.string.signature_update_signature_selected_method_id_card, 3, 3));

        TextUtil.setTextViewSizeInContainer(mobileIdRadioButton);
        TextUtil.setTextViewSizeInContainer(smartIdRadioButton);
        TextUtil.setTextViewSizeInContainer(idCardIdRadioButton);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public Observable<Integer> methodChanges() {
        return methodChanges;
    }

    public Observable<Boolean> positiveButtonEnabled() {
        return Observable
                .merge( methodChanges().startWith(Observable.fromCallable(this::method)),
                        idCardView.positiveButtonState(), mobileIdView.positiveButtonState(), smartIdView.positiveButtonState())
                .map(ignored ->
                        (method() == R.id.signatureUpdateSignatureAddMethodMobileId && mobileIdView.positiveButtonEnabled()) ||
                        (method() == R.id.signatureUpdateSignatureAddMethodSmartId && smartIdView.positiveButtonEnabled()) ||
                        (method() == R.id.signatureUpdateSignatureAddMethodIdCard && idCardView.positiveButtonEnabled()));
    }

    public int method() {
        return methodView.getCheckedRadioButtonId();
    }

    public void method(int method) {
        mobileIdView.setVisibility(method == R.id.signatureUpdateSignatureAddMethodMobileId ? VISIBLE : GONE);
        smartIdView.setVisibility(method == R.id.signatureUpdateSignatureAddMethodSmartId ? VISIBLE : GONE);
        idCardView.setVisibility(method == R.id.signatureUpdateSignatureAddMethodIdCard ? VISIBLE : GONE);
        switch (method) {
            case R.id.signatureUpdateSignatureAddMethodMobileId:
                mobileIdView.setDefaultPhoneNoPrefix("372");
                break;
            case R.id.signatureUpdateSignatureAddMethodSmartId:
            case R.id.signatureUpdateSignatureAddMethodIdCard:
                break;
            default:
                throw new IllegalArgumentException("Unknown method " + method);
        }
    }

    public void reset(SignatureUpdateViewModel viewModel) {
        methodView.check(viewModel.signatureAddMethod());
        idCardView.reset(viewModel);
        mobileIdView.reset(viewModel);
        smartIdView.reset(viewModel);
        mobileIdView.clearFocus();
        smartIdView.clearFocus();
        idCardView.clearFocus();
        methodView.requestFocus();
    }

    public SignatureAddRequest request() {
        switch (method()) {
            case R.id.signatureUpdateSignatureAddMethodMobileId:
                return mobileIdView.request();
            case R.id.signatureUpdateSignatureAddMethodSmartId:
                return smartIdView.request();
            case R.id.signatureUpdateSignatureAddMethodIdCard:
                return idCardView.request();
            default:
                throw new IllegalStateException("Unknown method " + method());
        }
    }

    public void response(SignatureAddResponse response) {
        if (response == null && mobileIdView.getVisibility() == VISIBLE) {
            mobileIdView.response(null, null);
        } else if (response == null && smartIdView.getVisibility() == VISIBLE) {
            smartIdView.response(null, methodView);
        } else if (response == null && idCardView.getVisibility() == VISIBLE) {
            idCardView.response(null, methodView);
        } else if (response instanceof MobileIdResponse) {
            mobileIdView.response((MobileIdResponse) response, null);
        } else if (response instanceof SmartIdResponse) {
            smartIdView.response((SmartIdResponse) response, null);
        } else if (response instanceof IdCardResponse) {
            idCardView.response((IdCardResponse) response, methodView);
        } else {
            throw new IllegalArgumentException("Unknown response " + response);
        }
    }

    private void setupContentDescriptions(RadioButton radioButton, String contentDescription) {
        radioButton.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
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

}
