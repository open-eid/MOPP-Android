package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import timber.log.Timber;

import static com.jakewharton.rxbinding2.widget.RxRadioGroup.checkedChanges;

public final class SignatureUpdateSignatureAddView extends LinearLayout {

    private final RadioGroup methodView;

    private final ViewDisposables disposables = new ViewDisposables();

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
        inflate(context, R.layout.signature_update_signature_add, this);
        methodView = findViewById(R.id.signatureUpdateSignatureAddMethod);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(checkedChanges(methodView)
                .subscribe(methodId -> Timber.e("UUJEE: %s", methodId)));
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
