package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.mobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.widget.RxTextView.afterTextChangeEvents;

public final class MobileIdView extends LinearLayout implements
        SignatureAddView<MobileIdRequest, MobileIdResponse> {

    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final EditText phoneNoView;
    private final EditText personalCodeView;
    private final CheckBox rememberMeView;

    public MobileIdView(Context context) {
        this(context, null);
    }

    public MobileIdView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MobileIdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MobileIdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                        int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_mobile_id, this);
        phoneNoView = findViewById(R.id.signatureUpdateMobileIdPhoneNo);
        personalCodeView = findViewById(R.id.signatureUpdateMobileIdPersonalCode);
        rememberMeView = findViewById(R.id.signatureUpdateMobileIdRememberMe);

        setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == VISIBLE) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_message);
            }
        });
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        phoneNoView.setText(viewModel.phoneNo());
        personalCodeView.setText(viewModel.personalCode());
        setDefaultCheckBoxToggle(viewModel);
    }

    @Override
    public MobileIdRequest request() {
        return MobileIdRequest.create(phoneNoView.getText().toString(),
                personalCodeView.getText().toString(), rememberMeView.isChecked());
    }

    @Override
    public void response(@Nullable MobileIdResponse response) {
        if (response != null && response.status() != null && response.status() == GetMobileCreateSignatureStatusResponse.ProcessStatus.SIGNATURE) {
            AccessibilityUtils.sendAccessibilityEvent(this.getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.container_signature_added);
        }
    }

    public void setDefaultPhoneNoPrefix(String phoneNoPrefix) {
        if (TextUtils.isEmpty(phoneNoView.getText())) {
            phoneNoView.setText(phoneNoPrefix, TextView.BufferType.EDITABLE);
            phoneNoView.setOnFocusChangeListener((view, hasfocus) -> {
                if (hasfocus) {
                    phoneNoView.setHint("372XXXXXXXX");
                } else {
                    phoneNoView.setHint("");
                }
            });
        }
    }

    private void setDefaultCheckBoxToggle(SignatureUpdateViewModel viewModel) {
        if (viewModel.phoneNo().length() > 0 && viewModel.personalCode().length() > 0) {
            rememberMeView.setChecked(true);
        } else {
            rememberMeView.setChecked(false);
        }
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(phoneNoView), afterTextChangeEvents(personalCodeView));
    }

    public boolean positiveButtonEnabled() {
        return phoneNoView.getText().length() > 3 && personalCodeView.getText().length() == 11;
    }
}
