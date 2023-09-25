package ee.ria.DigiDoc.android.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

public class ViewUtil {

    public static View findLastElement(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int subviews = viewGroup.getChildCount();
            if (subviews > 0) {
                return findLastElement(viewGroup.getChildAt(subviews - 1));
            }
        }

        return view;
    }

    public static View findMainLayoutElement(View view) {
        View mainView = findLastElement(view);

        if (mainView != null) {
            ViewParent parent = mainView.getParent();
            if (parent != null) {
                while ((parent instanceof LinearLayout ||
                        parent instanceof RelativeLayout ||
                        parent instanceof RecyclerView)) {
                    mainView = (View) parent;
                    parent = mainView.getParent();
                }
            }

        }
        return mainView;
    }

    public static void moveView(View view) {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                marginLayoutParams.setMargins(marginLayoutParams.leftMargin, -100, marginLayoutParams.rightMargin, marginLayoutParams.bottomMargin);
                view.setLayoutParams(layoutParams);
            }
        }
    }
}
