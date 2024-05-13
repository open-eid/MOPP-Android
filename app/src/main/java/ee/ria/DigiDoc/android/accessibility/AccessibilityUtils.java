package ee.ria.DigiDoc.android.accessibility;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.text.TextUtils.isEmpty;
import static java.util.Objects.nonNull;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.textfield.TextInputLayout;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.Locale;

import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.common.TextUtil;

public class AccessibilityUtils {

    public static boolean isAccessibilityEnabled() {
        Activity activity = (Activity) Activity.getContext().get();
        AccessibilityManager accessibilityManager = (AccessibilityManager) activity.getSystemService(ACCESSIBILITY_SERVICE);
        return accessibilityManager.isEnabled();
    }

    public static boolean isTalkBackEnabled() {
        Activity activity = (Activity) Activity.getContext().get();
        AccessibilityManager accessibilityManager = (AccessibilityManager) activity.getSystemService(ACCESSIBILITY_SERVICE);
        return accessibilityManager.isTouchExplorationEnabled();
    }

    public static AccessibilityManager.TouchExplorationStateChangeListener addAccessibilityStateChanged(AccessibilityManager.TouchExplorationStateChangeListener listener) {
        Activity activity = (Activity) Activity.getContext().get();
        AccessibilityManager accessibilityManager = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addTouchExplorationStateChangeListener(listener);
        return listener;
    }

    public static void removeAccessibilityStateChanged(AccessibilityManager.TouchExplorationStateChangeListener listener) {
        Activity activity = (Activity) Activity.getContext().get();
        AccessibilityManager accessibilityManager = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.removeTouchExplorationStateChangeListener(listener);
    }

    public static void sendAccessibilityEvent(Context context, int eventType, @StringRes int messageResId) {
        sendAccessibilityEvent(context, eventType, context.getString(messageResId));
    }

    public static void interrupt(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        accessibilityManager.interrupt();
    }

    public static void sendAccessibilityEvent(Context context, int eventType, CharSequence message) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = getAccessibilityEvent();
            event.setEventType(eventType);
            event.getText().add(message);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public static void sendAccessibilityEvent(Context context, int eventType, CharSequence... messages) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = getAccessibilityEvent();
            event.setEventType(eventType);
            event.getText().add(combineMessages(messages));
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    /** @noinspection unused*/
    public static void sendDelayedAccessibilityEvent(Context context, int eventType, CharSequence message) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = getAccessibilityEvent();
            event.setEventType(eventType);
            event.getText().add(message);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public static void setViewAccessibilityPaneTitle(View view, @StringRes int titleResId) {
        view.setAccessibilityPaneTitle(view.getResources().getString(titleResId).toLowerCase());
    }

    public static void setTextViewContentDescription(boolean setAsSingleCharacters, @Nullable String hint, String label, EditText editText) {
        editText.setAccessibilityDelegate(new View.AccessibilityDelegate() {

            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);

                if (info.getText() != null && setAsSingleCharacters) {
                    String text = AccessibilityUtils.getTextAsSingleCharacters(info.getText().toString());
                    info.setText(label + " " + text);
                    info.setContentDescription(label + " " + text);
                    host.setContentDescription(label + " " + text);
                } else {
                    info.setText(label);
                    info.setContentDescription(label);
                    host.setContentDescription(label);
                }

                if (editText.getText() != null && editText.getText().toString().isEmpty()) {
                    editText.setHint(hint);
                } else {
                    editText.setHint("");
                }
            }

            @Override
            public void onPopulateAccessibilityEvent(@NonNull View host, @NonNull AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);

                if (host instanceof EditText editText) {
                    int cursorPosition = editText.getSelectionStart();
                    int textLength = editText.getText().length();

                    String cursorPositionDescription;
                    if (cursorPosition == 0) {
                        cursorPositionDescription = "Cursor's focus is at the beginning";
                    } else if (cursorPosition == textLength) {
                        cursorPositionDescription = "Cursor's focus is at the end";
                    } else {
                        cursorPositionDescription = "Cursor's focus is at character " + (cursorPosition + 1);
                    }
                    if (editText.getText() != null && !editText.getText().toString().isEmpty()) {
                        if (setAsSingleCharacters) {
                            String text = AccessibilityUtils.getTextAsSingleCharacters(editText.getText().toString());
                            event.setContentDescription(String.format("%s %s, %s", label, text, cursorPositionDescription));
                        } else {
                            if (hint != null) {
                                editText.setHint("");
                            }
                            event.setContentDescription(String.format("%s %s, %s", label, editText.getText(), cursorPositionDescription));
                        }
                    } else {
                        if (hint != null) {
                            editText.setHint(hint);
                        }
                        event.getText().clear();
                        event.setContentDescription(String.format("%s %s, %s", label, editText.getText() != null ? editText.getText().toString() : hint, cursorPositionDescription));
                    }
                }
            }
        });
    }

    public static void setContentDescription(View view, @Nullable String text) {
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String contentDescription = "";
                if (text == null || text.isEmpty()) {
                    if (view instanceof TextView) {
                        contentDescription = ((TextView) view).getText().toString().toLowerCase();
                    } else if (view instanceof TextInputLayout) {
                        contentDescription = view.toString().toLowerCase();
                    }
                } else {
                    contentDescription = text.toLowerCase();
                }
                info.setText(contentDescription);
            }
        });
    }

    public static void disableDoubleTapToActivateFeedback(View view) {
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
                info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
            }
        });
    }

    public static StringBuilder getTextViewAccessibility(TextView textView) {
        StringBuilder textViewAccessibility = new StringBuilder();
        String[] personalCodeTextSplit = textView.getText().toString().split(",");
        for (String nameText : personalCodeTextSplit) {
            if (TextUtil.isOnlyDigits(nameText)) {
                textViewAccessibility.append(TextUtil.splitTextAndJoin(nameText, "", " "));
            } else {
                textViewAccessibility.append(nameText);
            }
        }

        return textViewAccessibility;
    }

    public static void setSingleCharactersContentDescription(TextView textView, @Nullable String title) {
        ViewCompat.setAccessibilityDelegate(textView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                StringBuilder textViewAccessibility = getTextViewAccessibility(textView);
                if (nonNull(title)) {
                    info.setText(title + " " + textViewAccessibility);
                    info.setContentDescription(title + " " + textViewAccessibility);
                    host.setContentDescription(title + " " + textViewAccessibility);
                } else {
                    info.setText(textViewAccessibility);
                    info.setContentDescription(textViewAccessibility);
                    host.setContentDescription(textViewAccessibility);
                }
            }
        });
    }

    public static void setSingleCharactersContentDescriptionWithHint(TextView textView, @Nullable String title, String hint) {
        ViewCompat.setAccessibilityDelegate(textView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                StringBuilder textViewAccessibility = getTextViewAccessibility(textView);
                if (isEmpty(textViewAccessibility)) {
                    textView.setHint(hint);
                    if (!isEmpty(textView.getHint())) {
                        textViewAccessibility = new StringBuilder(textView.getHint().toString());
                    }
                } else {
                    textView.setHint(" ");
                }
                if (nonNull(title)) {
                    info.setText(title + " " + textViewAccessibility);
                    info.setContentDescription(title + " " + textViewAccessibility);
                    host.setContentDescription(title + " " + textViewAccessibility);
                } else {
                    info.setText(textViewAccessibility);
                    info.setContentDescription(textViewAccessibility);
                    host.setContentDescription(textViewAccessibility);
                }
            }
        });
    }

    public static void setJoinedCharactersContentDescription(TextView textView) {
        ViewCompat.setAccessibilityDelegate(textView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);

                String joinedText = TextUtil.joinText(textView.getText().toString());

                info.setText(joinedText);
                info.setContentDescription(joinedText);
                host.setContentDescription(joinedText);
            }
        });
    }

    public static String getTextAsSingleCharacters(String text) {
        return TextUtil.splitTextAndJoin(text, "", " ");
    }

    public static void setEditTextCursorToEnd(EditText editText) {
        editText.post(() -> editText.setSelection(editText.getText().length()));
    }

    public static boolean isLargeFontEnabled(Resources resources) {
        Configuration configuration = resources.getConfiguration();
        return configuration.fontScale > 1;
    }

    public static boolean isSmallFontEnabled(Resources resources) {
        Configuration configuration = resources.getConfiguration();
        return configuration.fontScale < 1;
    }
    
    public static void setCustomClickAccessibilityFeedBack(TextView titleView, ExpandableLayout containerView) {
        ViewCompat.setAccessibilityDelegate(titleView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String message;
                if (containerView.isExpanded()) {
                    message = "deactivate";
                } else {
                    message = "activate";
                }
                AccessibilityNodeInfoCompat.AccessibilityActionCompat customClick = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK, message);
                info.addAction(customClick);
            }
        });
    }

    public static String getSignatureName(String text) {
        StringBuilder nameViewAccessibility = new StringBuilder();
        String[] nameTextSplit = text.split(", ");

        for (String nameText : nameTextSplit) {
            if (TextUtil.isOnlyDigits(nameText)) {
                nameViewAccessibility.append(TextUtil.splitTextAndJoin(nameText, "", " "));
            } else {
                nameViewAccessibility.append(nameText);
            }
        }

        return nameViewAccessibility.toString().toLowerCase();
    }

    public static String getButtonTranslation() {
        Locale systemLanguage = Resources.getSystem().getConfiguration().getLocales().get(0);
        return switch (systemLanguage.getLanguage()) {
            case "et" -> "nupp";
            case "ru" -> "кнопка";
            default -> "button";
        };
    }

    private static String combineMessages(CharSequence... messages) {
        StringBuilder combinedMessage = new StringBuilder();
        for (CharSequence message : messages) {
            combinedMessage.append(message).append(", ");
        }
        return combinedMessage.toString();
    }

    /** @noinspection deprecation*/
    private static AccessibilityEvent getAccessibilityEvent() {
        return new AccessibilityEvent();
    }
}
