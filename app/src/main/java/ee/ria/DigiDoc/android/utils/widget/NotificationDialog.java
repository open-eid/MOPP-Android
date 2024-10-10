package ee.ria.DigiDoc.android.utils.widget;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.sign.utils.UrlMessage;

public final class NotificationDialog extends AlertDialog implements ContentView,
        DialogInterface.OnClickListener {

    private final Context context;
    private final int action;
    private CheckBox dontShowAgainCheckbox;

    public NotificationDialog(@NonNull Context context, @StringRes int message, int action) {
        super(context);
        SecureUtil.markAsSecure(context, getWindow());

        this.context = context;

        if (action == R.id.nfcCanNotificationDialog) {
            int layoutPadding = 50;
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding);

            Spanned urlMessage = Html.fromHtml(UrlMessage.withURL(
                    context,
                    message,
                    R.string.read_more_message,
                    true
            ), Html.FROM_HTML_MODE_LEGACY);

            TextView messageView = new TextView(context);
            messageView.setTextAppearance(R.style.MaterialTypography_Dense_Body1);
            messageView.setLayoutParams(new ViewGroup.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT)
            );
            messageView.setText(urlMessage);
            messageView.setPadding(15, 0, 15, 25);
            messageView.setGravity(Gravity.START);

            messageView.setMovementMethod(LinkMovementMethod.getInstance());
            messageView.setClickable(true);

            messageView.setContentDescription(urlMessage);

            if (AccessibilityUtils.isTalkBackEnabled()) {
                messageView.setOnClickListener(view -> {
                    String messageWithUrl = context.getString(R.string.signature_update_nfc_can_info);
                    String url = UrlMessage.extractLink(messageWithUrl);
                    if (!url.isEmpty()) {
                        Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(context, uriIntent, null);
                    }
                });
            } else {
                messageView.setOnClickListener(null);
            }

            layout.addView(messageView);

            dontShowAgainCheckbox = new AppCompatCheckBox(context);
            dontShowAgainCheckbox.setId(android.R.id.checkbox);
            dontShowAgainCheckbox.setText(R.string.dont_show_again_message);
            layout.addView(dontShowAgainCheckbox);

            Button centerButton = new Button(context);
            centerButton.setText(context.getString(android.R.string.ok));
            centerButton.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            buttonLayoutParams.setMargins(15, 50, 15, 25);
            buttonLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            centerButton.setLayoutParams(buttonLayoutParams);

            centerButton.setOnClickListener(v -> onClick(this, DialogInterface.BUTTON_POSITIVE));

            layout.addView(centerButton);

            setView(layout);
        }
        else {
            setMessage(context.getString(message));
            setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
        }

        this.action = action;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (action == R.id.nfcCanNotificationDialog) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;

                boolean isChecked = dontShowAgainCheckbox.isChecked();
                activity.getSettingsDataStore().setShowCanMessage(!isChecked);
            }
        }
        dismiss();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String confirmationDialogDescription = getContext().getResources().getString(R.string.confirmation_dialog);
            event.getText().add(confirmationDialogDescription + ",");
            return true;
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }
}
