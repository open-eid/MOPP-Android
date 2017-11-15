package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import ee.ria.DigiDoc.R;

public final class HomeToolbar extends Toolbar {

    public HomeToolbar(Context context) {
        this(context, null);
    }

    public HomeToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public HomeToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
