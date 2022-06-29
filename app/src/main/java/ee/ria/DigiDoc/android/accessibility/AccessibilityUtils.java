package ee.ria.DigiDoc.android.accessibility;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.StringRes;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import ee.ria.DigiDoc.android.Activity;

import static android.content.Context.ACCESSIBILITY_SERVICE;

public class AccessibilityUtils {

    public static boolean isAccessibilityEnabled() {
        Activity activity = (Activity) Activity.getContext().get();
        AccessibilityManager accessibilityManager = (AccessibilityManager) activity.getSystemService(ACCESSIBILITY_SERVICE);
        return accessibilityManager.isEnabled();
    }

    public static void sendAccessibilityEvent(Context context, int eventType, @StringRes int messageResId) {
        sendAccessibilityEvent(context, eventType, context.getString(messageResId));
    }

    public static void sendAccessibilityEvent(Context context, int eventType, CharSequence message) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(eventType);
            event.getText().add(message);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public static void sendAccessibilityEvent(Context context, int eventType, CharSequence... messages) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(eventType);
            event.getText().add(combineMessages(messages));
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public static void sendDelayedAccessibilityEvent(Context context, int eventType, CharSequence message) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(eventType);
            event.getText().add(message);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public static void setViewAccessibilityPaneTitle(View view, @StringRes int titleResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityPaneTitle(view.getResources().getString(titleResId));
        }
    }

    public static void disableContentDescription(View view) {
        view.setContentDescription(null);
    }

    public static void disableDoubleTapToActivateFeedback(View view) {
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
            }
        });
    }

    private static String combineMessages(CharSequence... messages) {
        StringBuilder combinedMessage = new StringBuilder();
        for (CharSequence message : messages) {
            combinedMessage.append(message).append(", ");
        }
        return combinedMessage.toString();
    }
}
