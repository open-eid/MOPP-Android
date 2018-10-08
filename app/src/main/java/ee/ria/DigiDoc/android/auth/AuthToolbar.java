package ee.ria.DigiDoc.android.auth;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import ee.ria.DigiDoc.R;

public class AuthToolbar extends Toolbar {


    public AuthToolbar(Context context) {
        this(context, null);
    }

    public AuthToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public AuthToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.auth_toolbar, this);
    }
}

