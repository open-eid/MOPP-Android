package ee.ria.DigiDoc.android.accessibility;

import androidx.annotation.StringRes;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

public class EventFeedBackDelegate extends View.AccessibilityDelegate {

    private int messageResId;
    private boolean feedbackEventSent;

    public EventFeedBackDelegate(@StringRes int messageResId) {
        this.messageResId = messageResId;
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String message = host.getResources().getString(messageResId);
            event.getText().add(message);
            super.onPopulateAccessibilityEvent(host, event);
        }

        if (!feedbackEventSent) {
            super.onPopulateAccessibilityEvent(host, event);
        }
    }

    @Override
        public void sendAccessibilityEvent(View host, int eventType) {
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            feedbackEventSent = true;
            super.sendAccessibilityEvent(host, eventType);
        }

        // To prevent situation when event is fired after feedback event sending and
        // due to that feedback event message is ignored.
        if (!feedbackEventSent) {
            super.sendAccessibilityEvent(host, eventType);
        }
    }
}
