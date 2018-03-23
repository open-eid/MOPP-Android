package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;

public final class MobileIdView extends LinearLayout implements SignatureAddView<MobileIdRequest> {

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
    }

    @Override
    public void init(MobileIdRequest request) {
    }

    @Override
    public MobileIdRequest request() {
        return MobileIdRequest.create(phoneNoView.getText().toString(),
                personalCodeView.getText().toString(), rememberMeView.isChecked());
    }
}
