package ee.ria.DigiDoc.android.signature.add;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.R;

public final class SignatureAddView extends LinearLayout {

    private final EditText phoneNoView;
    private final EditText personalCodeView;
    private final CheckBox rememberMeView;

    public SignatureAddView(Context context) {
        this(context, null);
    }

    public SignatureAddView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureAddView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SignatureAddView(Context context, AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_add, this);
        phoneNoView = findViewById(R.id.signatureAddPhoneNo);
        personalCodeView = findViewById(R.id.signatureAddPersonalCode);
        rememberMeView = findViewById(R.id.signatureAddRememberMe);
    }

    public void setPhoneNo(String phoneNo) {
        phoneNoView.setText(phoneNo);
    }

    public void setPersonalCode(String personalCode) {
        personalCodeView.setText(personalCode);
    }

    Data getData() {
        return Data.create(phoneNoView.getText().toString(), personalCodeView.getText().toString(),
                rememberMeView.isChecked());
    }

    @AutoValue
    public static abstract class Data {

        public abstract String phoneNo();
        public abstract String personalCode();
        public abstract boolean rememberMe();

        static Data create(String phoneNo, String personalCode, boolean rememberMe) {
            return new AutoValue_SignatureAddView_Data(phoneNo, personalCode, rememberMe);
        }
    }
}
