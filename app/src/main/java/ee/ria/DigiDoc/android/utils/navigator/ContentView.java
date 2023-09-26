package ee.ria.DigiDoc.android.utils.navigator;

import static ee.ria.DigiDoc.android.utils.ViewUtil.findMainLayoutElement;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import ee.ria.DigiDoc.BuildConfig;

public interface ContentView {

    // Adds an invisible label element to the bottom of the view.
    // Used for autotests by testers
    static void addInvisibleElement(Context context, View view) {
        if (BuildConfig.DEBUG) {
            View mainLayoutElement = findMainLayoutElement(view);
            InvisibleView.addInvisibleElement(context, mainLayoutElement);
        }
    }

    static void addInvisibleElementToObject(Context context, View view) {
        if (BuildConfig.DEBUG) {
            InvisibleView.addInvisibleElementToObject(context, view);
        }
    }

    static void addInvisibleElementScrollListener(RecyclerView recyclerView, View lastElementView) {
        if (BuildConfig.DEBUG) {
            InvisibleView.addInvisibleElementScrollListener(recyclerView, lastElementView);
        }
    }

    static void removeInvisibleElementScrollListener(RecyclerView recyclerView) {
        if (BuildConfig.DEBUG) {
            InvisibleView.removeInvisibleElementScrollListener(recyclerView);
        }
    }
}
