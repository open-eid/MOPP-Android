package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
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
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.widget.RxRadioGroup.checkedChanges;

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
                AccessibilityUtils.sendAccessibilityEvent(getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, getContext().getString(R.string.signature_update_signature_chosen_method_mobile_id));
                mobileIdView.setDefaultPhoneNoPrefix("372");
                break;
            case R.id.signatureUpdateSignatureAddMethodSmartId:
                AccessibilityUtils.sendAccessibilityEvent(getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, getContext().getString(R.string.signature_update_signature_chosen_method_smart_id));
                break;
            case R.id.signatureUpdateSignatureAddMethodIdCard:
                AccessibilityUtils.sendAccessibilityEvent(getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, getContext().getString(R.string.signature_update_signature_chosen_method_id_card));
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
            mobileIdView.response(null);
        } else if (response == null && smartIdView.getVisibility() == VISIBLE) {
            smartIdView.response(null);
        } else if (response == null && idCardView.getVisibility() == VISIBLE) {
            idCardView.response(null);
        } else if (response instanceof MobileIdResponse) {
            mobileIdView.response((MobileIdResponse) response);
        } else if (response instanceof SmartIdResponse) {
            smartIdView.response((SmartIdResponse) response);
        } else if (response instanceof IdCardResponse) {
            idCardView.response((IdCardResponse) response);
        } else {
            throw new IllegalArgumentException("Unknown response " + response);
        }
    }
}
