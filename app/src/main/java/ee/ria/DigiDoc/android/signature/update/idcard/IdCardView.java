package ee.ria.DigiDoc.android.signature.update.idcard;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

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
        return positiveButtonEnabledSubject;
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
        IdCardData data = response == null ? null : response.data();
        if (response == null || !response.readerConnected()) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_reader);
            signContainerView.setVisibility(GONE);
            positiveButtonEnabledSubject.onNext(false);
        } else if (response.readerConnected() && data == null && !response.signingActive()) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_card);
            signContainerView.setVisibility(GONE);
            positiveButtonEnabledSubject.onNext(false);
        } else if (data != null && !response.signingActive()) {
            progressContainerView.setVisibility(GONE);
            signContainerView.setVisibility(VISIBLE);
            signDataView.setText(getResources().getString(
                    R.string.signature_update_id_card_sign_data, data.givenNames(), data.surname(),
                    data.personalCode()));
            if (response.error() == null && response.retryCounter() == null) {
                signPin2ErrorView.setVisibility(GONE);
            } else {
                signPin2ErrorView.setVisibility(VISIBLE);
                signPin2ErrorView.setText(getResources().getString(
                        R.string.signature_update_id_card_sign_pin2_invalid,
                        response.retryCounter()));
            }
            positiveButtonEnabledSubject.onNext(true);
        } else if (response.signingActive()) {
            progressContainerView.setVisibility(VISIBLE);
            progressMessageView.setText(R.string.signature_update_id_card_progress_message_signing);
            signContainerView.setVisibility(GONE);
            positiveButtonEnabledSubject.onNext(false);
        }
    }
}
