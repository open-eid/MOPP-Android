package ee.ria.DigiDoc.android.main.about;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarViewTitle;
import static ee.ria.DigiDoc.android.utils.navigator.ContentView.addInvisibleElement;
import static ee.ria.DigiDoc.android.utils.navigator.ContentView.addInvisibleElementScrollListener;
import static ee.ria.DigiDoc.android.utils.navigator.ContentView.removeInvisibleElementScrollListener;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.Controller;
import com.google.android.material.appbar.AppBarLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
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
    @SuppressLint("MissingInflatedId")
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        View view = inflater.inflate(R.layout.main_about_screen, container, false);
        AccessibilityUtils.setViewAccessibilityPaneTitle(view, R.string.main_about_title);

        Toolbar toolbarView = view.findViewById(R.id.toolbar);
        AppBarLayout appBarLayout = view.findViewById(R.id.appBar);
        toolbarView.setTitle(R.string.main_about_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);
        listView = view.findViewById(R.id.mainAboutList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(new AboutAdapter());
        listView.clearFocus();
        addInvisibleElement(getApplicationContext(), view);
        View lastElementView = view.findViewById(R.id.lastInvisibleElement);
        if (lastElementView != null) {
            addInvisibleElementScrollListener(listView, lastElementView);
        }

        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(ignored ->
                ApplicationApp.component(container.getContext()).navigator().execute(Transaction.pop())));


        TextView toolbarTitleView = getToolbarViewTitle(toolbarView);
        if (toolbarTitleView != null) {
            toolbarTitleView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        appBarLayout.postDelayed(() -> {
            appBarLayout.requestFocus();
            appBarLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            appBarLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        }, 1000);

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        disposables.detach();
        removeInvisibleElementScrollListener(listView);
        super.onDestroyView(view);
    }
}
