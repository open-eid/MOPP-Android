package ee.ria.DigiDoc.android.signature.update.idcard;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardSignResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardStatus;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.tokenlibrary.Token;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.widget.RxTextView.afterTextChangeEvents;
import static ee.ria.DigiDoc.android.Constants.VOID;

public final class IdCardView extends LinearLayout implements
        SignatureAddView<IdCardRequest, IdCardResponse> {

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View signContainerView;
    private final TextView signDataView;
    private final EditText signPin2View;
    private final TextView signPin2ErrorView;

    @Nullable private Token token;

    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();

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

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(signPin2View));
    }

    public boolean positiveButtonEnabled() {
        return token != null && signPin2View.getText().length() >= 4;
    }

    @Override
    public void reset(@Nullable SignatureUpdateViewModel viewModel) {
        signPin2View.setText(null);
    }

    @Override
    public IdCardRequest request() {
        return IdCardRequest.create(token, signPin2View.getText().toString());
    }

    @Override
    public void response(@Nullable IdCardResponse response) {
        IdCardDataResponse dataResponse = response == null ? null : response.dataResponse();
        IdCardSignResponse signResponse = response == null ? null : response.signResponse();

        if (signResponse != null && signResponse.state().equals(State.CLEAR)) {
            reset(null);
        }

        IdCardData data = dataResponse == null ? null : dataResponse.data();
        if (data == null && signResponse != null) {
            data = signResponse.data();
        }

        token = dataResponse == null ? null : dataResponse.token();
        if (token == null && signResponse != null) {
            token = signResponse.token();
        }
        positiveButtonStateSubject.onNext(VOID);

        if (signResponse != null && signResponse.state().equals(State.ACTIVE)) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(
                    R.string.signature_update_id_card_progress_message_signing);
            signContainerView.setVisibility(GONE);
        } else if (signResponse != null && signResponse.error() != null && data != null) {
            int pinRetryCount = data.signCertificate().pinRetryCount();
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.givenNames(), data.surname(),
                    data.personalCode()));
            signPin2ErrorView.setVisibility(VISIBLE);
            if (pinRetryCount == 1) {
                signPin2ErrorView.setText(
                        R.string.signature_update_id_card_sign_pin2_invalid_final);
            } else {
                signPin2ErrorView.setText(getResources().getString(
                        R.string.signature_update_id_card_sign_pin2_invalid, pinRetryCount));
            }
        } else if (dataResponse != null && data != null) {
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.givenNames(), data.surname(),
                    data.personalCode()));
            signPin2ErrorView.setVisibility(GONE);
        } else if (dataResponse != null
                && dataResponse.status().equals(IdCardStatus.CARD_DETECTED)) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(
                    R.string.signature_update_id_card_progress_message_card_detected);
            signContainerView.setVisibility(GONE);
        } else if (dataResponse != null
                && dataResponse.status().equals(IdCardStatus.READER_DETECTED)) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(
                    R.string.signature_update_id_card_progress_message_reader_detected);
            signContainerView.setVisibility(GONE);
        } else {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_initial);
            signContainerView.setVisibility(GONE);
        }
    }
}
