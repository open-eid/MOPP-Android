package ee.ria.DigiDoc.android.signature;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

import ee.ria.DigiDoc.R;

public final class SignatureHomeView extends CoordinatorLayout {

    public SignatureHomeView(Context context) {
        this(context, null);
    }

    public SignatureHomeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureHomeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_home, this);
    }
}
