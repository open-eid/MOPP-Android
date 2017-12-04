package ee.ria.DigiDoc.android.signature.home;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.widget.Button;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class SignatureHomeView extends CoordinatorLayout {

    private final Button createButton;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public SignatureHomeView(Context context) {
        this(context, null);
    }

    public SignatureHomeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureHomeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_home, this);
        createButton = findViewById(R.id.signatureHomeCreateButton);
        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(clicks(createButton).subscribe(o ->
                navigator.pushScreen(SignatureCreateScreen.create())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
