package ee.ria.DigiDoc.android.crypto.create;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.scardcomlibrary.SmartCardReaderStatus;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static ee.ria.DigiDoc.android.Constants.VOID;

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
                (dialog, which) -> positiveButtonClicksSubject.onNext(VOID));
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> cancel());
    }

    void idCardDataResponse(IdCardDataResponse idCardDataResponse) {
        IdCardData data = idCardDataResponse.data();

        if (data == null) {
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
        } else {
            progressContainerView.setVisibility(View.GONE);
            containerView.setVisibility(View.VISIBLE);
            dataView.setText(getContext().getString(R.string.crypto_create_decrypt_data,
                    data.givenNames(), data.surname(), data.personalCode()));
        }
    }

    Observable<String> positiveButtonClicks() {
        return positiveButtonClicksSubject
                .map(ignored -> pin1View.getText().toString());
    }
}
