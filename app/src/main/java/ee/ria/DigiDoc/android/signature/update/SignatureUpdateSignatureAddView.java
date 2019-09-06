package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardResponse;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardView;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdView;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.widget.RxRadioGroup.checkedChanges;

public final class SignatureUpdateSignatureAddView extends LinearLayout {

    private final RadioGroup methodView;
    private final MobileIdView mobileIdView;
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
        idCardView = findViewById(R.id.signatureUpdateIdCard);
        methodChanges = checkedChanges(methodView).skipInitialValue().publish().autoConnect();
    }

    public Observable<Integer> methodChanges() {
        return methodChanges;
    }

    public Observable<Boolean> positiveButtonEnabled() {
        return Observable
                .merge( methodChanges().startWith(Observable.fromCallable(this::method)),
                        idCardView.positiveButtonState(), mobileIdView.positiveButtonState())
                .map(ignored ->
                        (method() == R.id.signatureUpdateSignatureAddMethodMobileId && mobileIdView.positiveButtonEnabled())
                                || idCardView.positiveButtonEnabled());
    }

    public int method() {
        return methodView.getCheckedRadioButtonId();
    }

    public void method(int method) {
        switch (method) {
            case R.id.signatureUpdateSignatureAddMethodMobileId:
                mobileIdView.setVisibility(VISIBLE);
                idCardView.setVisibility(GONE);
                break;
            case R.id.signatureUpdateSignatureAddMethodIdCard:
                mobileIdView.setVisibility(GONE);
                idCardView.setVisibility(VISIBLE);
                break;
            default:
                throw new IllegalArgumentException("Unknown method " + method);
        }
    }

    public void reset(SignatureUpdateViewModel viewModel) {
        methodView.check(R.id.signatureUpdateSignatureAddMethodMobileId);
        if (idCardView.getVisibility() == VISIBLE) {
            idCardView.reset(viewModel);
        } else {
            idCardView.reset(viewModel);
            mobileIdView.reset(viewModel);
        }
    }

    public SignatureAddRequest request() {
        switch (method()) {
            case R.id.signatureUpdateSignatureAddMethodMobileId:
                return mobileIdView.request();
            case R.id.signatureUpdateSignatureAddMethodIdCard:
                return idCardView.request();
            default:
                throw new IllegalStateException("Unknown method " + method());
        }
    }

    public void response(SignatureAddResponse response) {
        if (response == null && mobileIdView.getVisibility() == VISIBLE) {
            mobileIdView.response(null);
        } else if (response == null && idCardView.getVisibility() == VISIBLE) {
            idCardView.response(null);
        } else if (response instanceof MobileIdResponse) {
            mobileIdView.response((MobileIdResponse) response);
        } else if (response instanceof IdCardResponse) {
            idCardView.response((IdCardResponse) response);
        } else {
            throw new IllegalArgumentException("Unknown response " + response);
        }
    }

    public boolean isMobileIdAsSigningMethodSelected() {
        RadioButton selectedRadioButton = findViewById(methodView.getCheckedRadioButtonId());
        return selectedRadioButton.getText().equals("Mobile-ID");
    }
}
