package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class IdCardView extends LinearLayout implements
        SignatureUpdateSignatureAddView.SignatureAddView {

    public IdCardView(Context context) {
        this(context, null);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                      int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TextView view = new TextView(context);
        view.setGravity(Gravity.CENTER);
        view.setText("ID CARD YO");
        addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    public SignatureAddData data() {
        return SignatureAddData.idCard();
    }
}
