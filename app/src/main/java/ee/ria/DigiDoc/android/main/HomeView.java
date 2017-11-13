package ee.ria.DigiDoc.android.main;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.crypto.CryptoHomeView;
import ee.ria.DigiDoc.android.eid.EIDHomeView;
import ee.ria.DigiDoc.android.signature.SignatureHomeView;
import io.reactivex.disposables.CompositeDisposable;

import static com.jakewharton.rxbinding2.support.design.widget.RxBottomNavigationView.itemSelections;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;

public final class HomeView extends LinearLayout {

    private final BottomNavigationView navigationView;

    @Nullable private CompositeDisposable disposables;

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
        navigationView = findViewById(R.id.mainHomeNavigation);
    }

    void navigationItemChanged(MenuItem item) {
        View current = getChildAt(0);
        @Nullable View replacement = null;

        if (item.getItemId() == R.id.mainHomeNavigationSignature
                && current.getId() != R.id.signatureHome) {
            replacement = new SignatureHomeView(getContext());
            replacement.setId(R.id.signatureHome);
        } else if (item.getItemId() == R.id.mainHomeNavigationCrypto
                && current.getId() != R.id.cryptoHome) {
            replacement = new CryptoHomeView(getContext());
            replacement.setId(R.id.cryptoHome);
        } else if (item.getItemId() == R.id.mainHomeNavigationEID
                && current.getId() != R.id.eidHome) {
            replacement = new EIDHomeView(getContext());
            replacement.setId(R.id.eidHome);
        }

        if (replacement != null) {
            ViewGroup.LayoutParams params = current.getLayoutParams();
            removeView(current);
            addView(replacement, 0, params);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (disposables != null) {
            disposables.dispose();
        }
        disposables = new CompositeDisposable();

        disposables.add(itemSelections(navigationView)
                .filter(duplicates())
                .subscribe(this::navigationItemChanged));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (disposables != null) {
            disposables.dispose();
            disposables = null;
        }
        super.onDetachedFromWindow();
    }
}
