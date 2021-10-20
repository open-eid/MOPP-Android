package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.annotation.Nullable;
import android.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.ImageButton;

import ee.ria.DigiDoc.R;
import io.reactivex.rxjava3.core.Observable;

import static com.jakewharton.rxbinding4.view.RxView.clicks;

public final class HomeToolbar extends Toolbar {

    private final ImageButton overflowButton;

    public HomeToolbar(Context context) {
        this(context, null);
    }

    public HomeToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public HomeToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_home_toolbar, this);

        overflowButton = findViewById(R.id.mainHomeToolbarOverflow);
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        overflowButton.setImageTintList(ColorStateList.valueOf(a.getColor(0, Color.BLACK)));
        a.recycle();
    }

    public Observable overflowButtonClicks() {
        return clicks(overflowButton);
    }
}
