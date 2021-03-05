package ee.ria.DigiDoc.android.crypto.create;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.idcard.Token;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static com.jakewharton.rxbinding2.widget.RxTextView.afterTextChangeEvents;

final class DecryptDialog extends AlertDialog {

    private final Subject<Object> positiveButtonClicksSubject = PublishSubject.create();

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View containerView;
    private final TextView dataView;
    private final EditText pin1View;
    private final TextView pin1ErrorView;

    private final ViewDisposables disposables = new ViewDisposables();

    private Token token = null;

    DecryptDialog(@NonNull Context context) {
        super(context);
        SecureUtil.markAsSecure(getWindow());
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext())
                .inflate(R.layout.crypto_create_decrypt_dialog, null);
        progressContainerView = view.findViewById(R.id.cryptoCreateDecryptProgressContainer);
        progressMessageView = view.findViewById(R.id.cryptoCreateDecryptProgressMessage);
        containerView = view.findViewById(R.id.cryptoCreateDecryptContainer);
        dataView = view.findViewById(R.id.cryptoCreateDecryptData);
        pin1View = view.findViewById(R.id.cryptoCreateDecryptPin1);
        pin1ErrorView = view.findViewById(R.id.cryptoCreateDecryptPin1Error);
        setView(view, padding, padding, padding, padding);

        setButton(BUTTON_POSITIVE,
                getContext().getString(R.string.crypto_create_decrypt_positive_button),
                (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> {
                    cancel();
                    AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.file_decryption_cancelled);
                }
        );
    }

    void idCardDataResponse(IdCardDataResponse idCardDataResponse, @State String decryptState,
                            @Nullable Throwable decryptError) {
        IdCardData data = idCardDataResponse.data();
        token = idCardDataResponse.token();

        if (decryptState.equals(State.CLEAR)) {
            pin1View.setText(null);
        }

        if (decryptState.equals(State.ACTIVE)) {
            progressContainerView.setVisibility(View.VISIBLE);
            progressMessageView.setText(R.string.crypto_create_decrypt_active);
            containerView.setVisibility(View.GONE);
            pin1ErrorView.setVisibility(View.GONE);
        } else if (data == null) {
            progressContainerView.setVisibility(View.VISIBLE);
            switch (idCardDataResponse.status()) {
                case IDLE:
                    progressMessageView.setText(
                            R.string.crypto_create_decrypt_progress_message_initial);
                    break;
                case READER_DETECTED:
                    progressMessageView.setText(
                            R.string.crypto_create_decrypt_progress_message_reader_detected);
                    break;
                case CARD_DETECTED:
                    progressMessageView.setText(
                            R.string.crypto_create_decrypt_progress_message_card_detected);
                    break;
            }
            containerView.setVisibility(View.GONE);
            pin1ErrorView.setVisibility(View.GONE);
        } else {
            int pin1RetryCount = data.pin1RetryCount();
            if (decryptError != null && decryptError instanceof Pin1InvalidException
                    && pin1RetryCount > 0) {
                if (pin1RetryCount == 1) {
                    pin1ErrorView.setText(getContext().getString(
                            R.string.crypto_create_decrypt_pin1_invalid_final));
                } else {
                    pin1ErrorView.setText(getContext().getString(
                            R.string.crypto_create_decrypt_pin1_invalid, pin1RetryCount));
                }
                pin1ErrorView.setVisibility(View.VISIBLE);
                pin1ErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            } else {
                pin1ErrorView.setVisibility(View.GONE);
            }
            progressContainerView.setVisibility(View.GONE);
            containerView.setVisibility(View.VISIBLE);
            dataView.setText(getContext().getString(R.string.crypto_create_decrypt_data,
                    data.personalData().givenNames(), data.personalData().surname(),
                    data.personalData().personalCode()));
        }

        if (progressContainerView.getVisibility() == View.VISIBLE) {
            AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, progressMessageView.getText());
        }
        
        if (pin1ErrorView.getVisibility() == View.VISIBLE) {
            AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, pin1ErrorView.getText());
        } else if (containerView.getVisibility() == View.VISIBLE) {
            String readyToSignDesc = containerView.getResources().getString(R.string.crypto_create_decrypt_message);
            CharSequence signerInfo = getContext().getString(R.string.crypto_create_decrypt_data,
                    data.personalData().givenNames(), data.personalData().surname(),
                    " Personal code " + data.personalData().personalCode());
            String enterPin1Desc = containerView.getResources().getString(R.string.crypto_create_decrypt_pin1);
            AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, readyToSignDesc, signerInfo, enterPin1Desc);
        }
    }

    Observable<String> positiveButtonClicks() {
        return positiveButtonClicksSubject
                .map(ignored -> pin1View.getText().toString());
    }

    private Observable<Object> pin1FieldChange() {
        return Observable.merge(positiveButtonClicks(), afterTextChangeEvents(pin1View));
    }

    private Observable<Boolean> positiveButtonEnabled() {
        return pin1FieldChange().map(ignored -> token != null && pin1View.getText().length() >= 4);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Button confirmButton = getButton(BUTTON_POSITIVE);
        if (confirmButton != null) {
            confirmButton.setContentDescription(getContext().getString(R.string.decrypt_button_description));
        }
        Button cancelButton = getButton(BUTTON_NEGATIVE);
        if (cancelButton != null) {
            cancelButton.setContentDescription(getContext().getString(R.string.cancel_decryption));
        }

        disposables.attach();
        disposables.add(positiveButtonEnabled().subscribe(enabled -> {
            Button decryptButton = getButton(BUTTON_POSITIVE);
            if (decryptButton != null) {
                decryptButton.setEnabled(enabled);
                if (enabled) {
                    clicks(decryptButton).subscribe(positiveButtonClicksSubject);
                }
            }
        }));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
