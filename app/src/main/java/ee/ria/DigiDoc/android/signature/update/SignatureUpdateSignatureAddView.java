package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import ee.ria.DigiDoc.R;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.widget.RxRadioGroup.checkedChanges;

public final class SignatureUpdateSignatureAddView extends LinearLayout {

    private static final SparseIntArray METHOD_IDS = new SparseIntArray();
    static {
        METHOD_IDS.put(R.id.signatureUpdateSignatureAddMethodMobileId,
                R.id.signatureUpdateMobileId);
        METHOD_IDS.put(R.id.signatureUpdateSignatureAddMethodIdCard, R.id.signatureUpdateIdCard);
    }

    private final RadioGroup methodView;

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
    }

    public Observable<Integer> methodChanges() {
        return checkedChanges(methodView).skipInitialValue();
    }

    public void method(int method) {
        int id = METHOD_IDS.get(method);
        if (findViewById(id) != null) {
            return;
        }
        View view;
        switch (method) {
            case R.id.signatureUpdateSignatureAddMethodMobileId:
                view = new MobileIdView(getContext());
                break;
            case R.id.signatureUpdateSignatureAddMethodIdCard:
                view = new IdCardView(getContext());
                break;
            default:
                throw new IllegalArgumentException("Unknown method " + method);
        }
        view.setId(id);
        if (getChildCount() == 2) {
            removeViewAt(1);
        }
        addView(view, 1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }
}
