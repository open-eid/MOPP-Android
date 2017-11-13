package ee.ria.DigiDoc.android.main;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;

public final class HomeView extends LinearLayout {

    public HomeView(Context context) {
        this(context, null);
    }

    public HomeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HomeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.main_home, this);
    }
}
