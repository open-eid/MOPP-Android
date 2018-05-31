package ee.ria.DigiDoc.android.crypto.create;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

public final class CryptoRecipientsScreen extends Controller implements Screen {

    public static CryptoRecipientsScreen create() {
        return new CryptoRecipientsScreen();
    }

    private View activityOverlayView;
    private View activityIndicatorView;

    @SuppressWarnings("WeakerAccess")
    public CryptoRecipientsScreen() {}

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : View.GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : View.GONE);
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.crypto_recipients_screen, container, false);
        RecyclerView listView = view.findViewById(R.id.cryptoRecipientsList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(new CryptoRecipientsAdapter());
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);

        setActivity(false);

        return view;
    }
}
