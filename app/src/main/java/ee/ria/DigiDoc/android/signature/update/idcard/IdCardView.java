package ee.ria.DigiDoc.android.signature.update.idcard;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardStatus;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.widget.RxTextView.afterTextChangeEvents;

public final class IdCardView extends LinearLayout implements
        SignatureAddView<IdCardRequest, IdCardResponse> {

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View signContainerView;
    private final TextView signDataView;
    private final EditText signPin2View;
    private final TextView signPin2ErrorView;

    private final Subject<Boolean> positiveButtonEnabledSubject = PublishSubject.create();

    public IdCardView(Context context) {
        this(context, null);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IdCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                      int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_id_card, this);
        progressContainerView = findViewById(R.id.signatureUpdateIdCardProgressContainer);
        progressMessageView = findViewById(R.id.signatureUpdateIdCardProgressMessage);
        signContainerView = findViewById(R.id.signatureUpdateIdCardSignContainer);
        signDataView = findViewById(R.id.signatureUpdateIdCardSignData);
        signPin2View = findViewById(R.id.signatureUpdateIdCardSignPin2);
        signPin2ErrorView = findViewById(R.id.signatureUpdateIdCardSignPin2Error);
    }

    public Observable<Boolean> positiveButtonEnabled() {
        return Observable.merge(
                afterTextChangeEvents(signPin2View).map(event -> {
                    Editable editable = event.editable();
                    return editable != null && editable.length() >= 4;
                }),
                positiveButtonEnabledSubject
                        .map(enabled-> enabled && signPin2View.getText().length() >= 4));
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        signPin2View.setText(null);
    }

    @Override
    public IdCardRequest request() {
        return IdCardRequest.create(signPin2View.getText().toString());
    }

    @Override
    public void response(@Nullable IdCardResponse response) {
        IdCardDataResponse dataResponse = response == null ? null : response.dataResponse();
        IdCardData data = dataResponse == null ? null : dataResponse.data();
        if (response == null || dataResponse == null
                || dataResponse.status().equals(IdCardStatus.INITIAL)) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_initial);
            signContainerView.setVisibility(GONE);
            positiveButtonEnabledSubject.onNext(false);
        } else if (dataResponse.status().equals(IdCardStatus.READER_DETECTED)) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(
                    R.string.signature_update_id_card_progress_message_reader_detected);
            signContainerView.setVisibility(GONE);
            positiveButtonEnabledSubject.onNext(false);
        } else if (dataResponse.status().equals(IdCardStatus.CARD_DETECTED) && data == null) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(
                    R.string.signature_update_id_card_progress_message_card_detected);
            signContainerView.setVisibility(GONE);
            positiveButtonEnabledSubject.onNext(false);
        } else if (dataResponse.status().equals(IdCardStatus.CARD_DETECTED) && data != null) {
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.givenNames(), data.surname(),
                    data.personalCode()));
            positiveButtonEnabledSubject.onNext(true);
        }

//        IdCardData data = response == null ? null : response.data();
//        if (response == null || !response.readerConnected()) {
//            progressContainerView.setVisibility(VISIBLE);
//            progressMessageView.setText(R.string.signature_update_id_card_progress_message_reader);
//            signContainerView.setVisibility(GONE);
//            positiveButtonEnabledSubject.onNext(false);
//        } else if (response.readerConnected() && data == null && !response.signingActive()) {
//            progressContainerView.setVisibility(VISIBLE);
//            progressMessageView.setText(R.string.signature_update_id_card_progress_message_card);
//            signContainerView.setVisibility(GONE);
//            positiveButtonEnabledSubject.onNext(false);
//        } else if (data != null && !response.signingActive()) {
//            progressContainerView.setVisibility(GONE);
//            signContainerView.setVisibility(VISIBLE);
//            signDataView.setText(getResources().getString(
//                    R.string.signature_update_id_card_sign_data, data.givenNames(), data.surname(),
//                    data.personalCode()));
//            if (response.error() == null && response.retryCounter() == null) {
//                signPin2ErrorView.setVisibility(GONE);
//            } else {
//                signPin2ErrorView.setVisibility(VISIBLE);
//                signPin2ErrorView.setText(getResources().getString(
//                        R.string.signature_update_id_card_sign_pin2_invalid,
//                        response.retryCounter()));
//            }
//            positiveButtonEnabledSubject.onNext(true);
//        } else if (response.signingActive()) {
//            progressContainerView.setVisibility(VISIBLE);
//            progressMessageView.setText(R.string.signature_update_id_card_progress_message_signing);
//            signContainerView.setVisibility(GONE);
//            positiveButtonEnabledSubject.onNext(false);
//        }
    }
}
