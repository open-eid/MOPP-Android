package ee.ria.DigiDoc.android.utils.navigator;

import static ee.ria.DigiDoc.android.utils.TextUtil.getInvisibleElementTextView;
import static ee.ria.DigiDoc.android.utils.ViewUtil.findLastElement;
import static ee.ria.DigiDoc.android.utils.ViewUtil.findMainLayoutElement;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public interface ContentView {

    // Adds an invisible label element to the bottom of the view.
    // Used for autotests by testers
    default void addInvisibleElement(Context context, View view) {
        View mainLayoutElement = findMainLayoutElement(view);
        if (mainLayoutElement instanceof ViewGroup) {
            ((ViewGroup) mainLayoutElement).addView(getInvisibleElementTextView(context));
        } else if (mainLayoutElement != null && mainLayoutElement.getParent() instanceof ViewGroup) {
            ((ViewGroup) mainLayoutElement.getParent()).addView(getInvisibleElementTextView(context));
        }
    }

    static void addInvisibleElementToObject(Context context, View view) {
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).addView(getInvisibleElementTextView(context));
        } else if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).addView(getInvisibleElementTextView(context));
        }
    }
}
