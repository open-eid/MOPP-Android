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

public final class CryptoCreateScreen extends Controller implements Screen {

    public static CryptoCreateScreen create() {
        return new CryptoCreateScreen();
    }

    private CryptoCreateAdapter adapter;

    @SuppressWarnings("WeakerAccess")
    public CryptoCreateScreen() {}

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.crypto_create_screen, container, false);
        RecyclerView listView = view.findViewById(R.id.cryptoCreateList);
        listView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());
        return view;
    }
}
