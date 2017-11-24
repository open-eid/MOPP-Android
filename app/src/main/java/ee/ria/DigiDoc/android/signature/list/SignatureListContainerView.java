package ee.ria.DigiDoc.android.signature.list;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

public final class SignatureListContainerView extends CardView {

    public SignatureListContainerView(Context context) {
        this(context, null);
    }

    public SignatureListContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureListContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
