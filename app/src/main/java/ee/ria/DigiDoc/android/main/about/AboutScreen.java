package ee.ria.DigiDoc.android.main.about;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.utils.navigator.ContentView.addInvisibleElement;
import static ee.ria.DigiDoc.android.utils.navigator.ContentView.addInvisibleElementScrollListener;
import static ee.ria.DigiDoc.android.utils.navigator.ContentView.removeInvisibleElementScrollListener;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.Controller;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class AboutScreen extends Controller implements Screen {

    public static AboutScreen create() {
        return new AboutScreen();
    }

    private RecyclerView listView;

    private final ViewDisposables disposables = new ViewDisposables();

    @SuppressWarnings("WeakerAccess")
    public AboutScreen() {
        super();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        View view = inflater.inflate(R.layout.main_about_screen, container, false);
        AccessibilityUtils.setViewAccessibilityPaneTitle(view, R.string.main_about_title);

        Toolbar toolbarView = view.findViewById(R.id.toolbar);
        toolbarView.setTitle(R.string.main_about_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);
        listView = view.findViewById(R.id.mainAboutList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(new AboutAdapter());

        addInvisibleElement(getApplicationContext(), view);
        View lastElementView = view.findViewById(R.id.lastInvisibleElement);
        addInvisibleElementScrollListener(listView, lastElementView);

        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(ignored ->
                Application.component(container.getContext()).navigator().execute(Transaction.pop())));

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        disposables.detach();
        removeInvisibleElementScrollListener(listView);
        super.onDestroyView(view);
    }
}
