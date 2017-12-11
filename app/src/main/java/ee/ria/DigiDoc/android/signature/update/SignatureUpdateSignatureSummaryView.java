package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;

public final class SignatureUpdateSignatureSummaryView extends LinearLayout {

    public SignatureUpdateSignatureSummaryView(Context context) {
        this(context, null);
    }

    public SignatureUpdateSignatureSummaryView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureUpdateSignatureSummaryView(Context context, @Nullable AttributeSet attrs,
                                               int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SignatureUpdateSignatureSummaryView(Context context, @Nullable AttributeSet attrs,
                                               int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        inflate(context, R.layout.signature_update_signature_summary, this);
    }
}
