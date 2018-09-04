package ee.ria.DigiDoc.android.auth;


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
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class AuthenticationDialog extends AlertDialog {

    private final Subject<Object> positiveButtonClicksSubject = PublishSubject.create();

    private final View progressContainerView;
    private final TextView progressMessageView;
    private final View containerView;
    private final TextView dataView;
    private final TextView verificationText;
    private final EditText pin1View;

    private final TextView pin1ErrorView;

    AuthenticationDialog(@NonNull Context context, String verificationCode) {
        super(context);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.dialogPreferredPadding});
        int padding = a.getDimensionPixelSize(0, 0);
        a.recycle();

        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext())
                .inflate(R.layout.auth_create_authentication_dialog, null);
        progressContainerView = view.findViewById(R.id.authCreateAuthenticateProgressContainer);
        progressMessageView = view.findViewById(R.id.authCreateAuthenticateProgressMessage);
        containerView = view.findViewById(R.id.authCreateAuthenticateContainer);
        dataView = view.findViewById(R.id.authCreateAuthenticateData);
        verificationText = view.findViewById(R.id.authCreateAuthenticateVerificationCode);
        pin1View = view.findViewById(R.id.authCreateAuthenticatePin1);
        pin1ErrorView = view.findViewById(R.id.authCreateAuthenticatePin1Error);
        setView(view, padding, padding, padding, padding);
        verificationText.setText(verificationCode);
        setButton(BUTTON_POSITIVE,
                getContext().getString(R.string.auth_create_authenticate_positive_button),
                (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel),
                (dialog, which) -> cancel());
    }

    void idCardDataResponse(IdCardDataResponse idCardDataResponse, @State String authenticationState,
                            @Nullable Throwable authenticationError) {


        if (authenticationState.equals(State.CLEAR)) {
            pin1View.setText(null);
        }

        IdCardData data = idCardDataResponse.data();
        if (authenticationState.equals(State.ACTIVE)) {
            progressContainerView.setVisibility(View.VISIBLE);
            progressMessageView.setText(R.string.auth_create_authenticate_active);
            containerView.setVisibility(View.GONE);
            pin1ErrorView.setVisibility(View.GONE);

        } else if (data == null) {
            progressContainerView.setVisibility(View.VISIBLE);
            switch (idCardDataResponse.status()) {
                case IDLE:
                    progressMessageView.setText(
                            R.string.auth_create_authenticate_progress_message_initial);
                    break;
                case READER_DETECTED:
                    progressMessageView.setText(
                            R.string.auth_create_authenticate_progress_message_reader_detected);
                    break;
                case CARD_DETECTED:
                    progressMessageView.setText(
                            R.string.auth_create_authenticate_progress_message_card_detected);
                    break;
            }
            containerView.setVisibility(View.GONE);
            pin1ErrorView.setVisibility(View.GONE);
        } else {
            int pin1RetryCount = data.pin1RetryCount();
            if (authenticationError != null && authenticationError instanceof Pin1InvalidException
                    && pin1RetryCount > 0) {
                if (pin1RetryCount == 1) {
                    pin1ErrorView.setText(getContext().getString(
                            R.string.auth_create_authenticate_pin1_invalid_final));
                } else {
                    pin1ErrorView.setText(getContext().getString(
                            R.string.auth_create_authenticate_pin1_invalid, pin1RetryCount));
                }
                pin1ErrorView.setVisibility(View.VISIBLE);
            } else {
                pin1ErrorView.setVisibility(View.GONE);
            }
            progressContainerView.setVisibility(View.GONE);
            containerView.setVisibility(View.VISIBLE);
            dataView.setText(getContext().getString(R.string.auth_create_authenticate_data,
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
