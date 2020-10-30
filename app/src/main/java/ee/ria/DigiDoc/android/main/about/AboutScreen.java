package ee.ria.DigiDoc.android.main.about;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Screen;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class AboutScreen extends Controller implements Screen {

    public static AboutScreen create() {
        return new AboutScreen();
    }

    private final ViewDisposables disposables = new ViewDisposables();

    @SuppressWarnings("WeakerAccess")
    public AboutScreen() {
        super();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.main_about_screen, container, false);
        AccessibilityUtils.setAccessibilityPaneTitle(view, R.string.main_about_title);

        Toolbar toolbarView = view.findViewById(R.id.toolbar);
        RecyclerView listView = view.findViewById(R.id.mainAboutList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(new AboutAdapter());

        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(ignored ->
                Application.component(container.getContext()).navigator().onBackPressed()));

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        disposables.detach();
        super.onDestroyView(view);
    }
}
