package ee.ria.DigiDoc.android.crypto.create;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderStatus;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

final class DecryptDialog extends AlertDialog {

    private final Subject<Object> positiveButtonClicksSubject = PublishSubject.create();

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View containerView;
    private final TextView dataView;
    private final EditText pin1View;
    private final TextView pin1ErrorView;

    DecryptDialog(@NonNull Context context) {
        super(context);
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
                (dialog, which) -> cancel());
    }

    void idCardDataResponse(IdCardDataResponse idCardDataResponse, @State String decryptState,
                            @Nullable Throwable decryptError) {
        IdCardData data = idCardDataResponse.data();

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
                case SmartCardReaderStatus.IDLE:
                    progressMessageView.setText(
                            R.string.crypto_create_decrypt_progress_message_initial);
                    break;
                case SmartCardReaderStatus.READER_DETECTED:
                    progressMessageView.setText(
                            R.string.crypto_create_decrypt_progress_message_reader_detected);
                    break;
                case SmartCardReaderStatus.CARD_DETECTED:
                    progressMessageView.setText(
                            R.string.crypto_create_decrypt_progress_message_card_detected);
                    break;
            }
            containerView.setVisibility(View.GONE);
            pin1ErrorView.setVisibility(View.GONE);
        } else {
            if (decryptError != null
                    && decryptError instanceof IdCardService.PinVerificationError) {
                data = ((IdCardService.PinVerificationError) decryptError).idCardData;
                pin1ErrorView.setText(getContext().getString(
                        R.string.crypto_create_decrypt_pin1_invalid, data.pin1RetryCount()));
                pin1ErrorView.setVisibility(View.VISIBLE);
            } else {
                pin1ErrorView.setVisibility(View.GONE);
            }
            progressContainerView.setVisibility(View.GONE);
            containerView.setVisibility(View.VISIBLE);
            dataView.setText(getContext().getString(R.string.crypto_create_decrypt_data,
                    data.personalData().givenNames(), data.personalData().surname(),
                    data.personalData().personalCode()));
        }
    }

    Observable<String> positiveButtonClicks() {
        return positiveButtonClicksSubject
                .map(ignored -> pin1View.getText().toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // override default click listener to prevent dialog dismiss
        clicks(getButton(BUTTON_POSITIVE)).subscribe(positiveButtonClicksSubject);
    }
}
