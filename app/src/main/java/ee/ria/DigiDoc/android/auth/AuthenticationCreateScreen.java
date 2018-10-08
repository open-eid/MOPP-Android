package ee.ria.DigiDoc.android.auth;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.bluelinelabs.conductor.Controller;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.VerificationCodeCalculator;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_INTENT;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

public class AuthenticationCreateScreen extends Controller implements Screen,
        MviView<Intent, ViewState> {

    private static final String KEY_SESSION_ID = "sessionId";
    @Nullable
    private final android.content.Intent intent;
    private ByteBuffer signature;
    private String sessionId;
    private String hash;
    private AuthenticationDialog authenticationDialog;
    private final Subject<Boolean> idCardTokenAvailableSubject = PublishSubject.create();
    private final ViewDisposables disposables = new ViewDisposables();
    private AuthCreateViewModel viewModel;
    private Button authButton;
    private Button closeButton;
    private Context context;
    private TextView successMessage;
    private AlertDialog errorDialog;
    private IdCardDataResponse authenticationIdCardDataResponse;
    @Nullable
    private Throwable authenticationError;

    public static AuthenticationCreateScreen create(String hash, String sessionId) {
        Bundle args = new Bundle();
        args.putString(KEY_AUTHTOKEN, hash);
        args.putString(KEY_SESSION_ID, sessionId);
        return new AuthenticationCreateScreen(args);
    }

    public AuthenticationCreateScreen(Bundle args) {
        super(args);
        hash = args.getString(KEY_AUTHTOKEN);
        sessionId = args.getString(KEY_SESSION_ID);
        intent = args.getParcelable(KEY_INTENT);
    }

    private Observable<Intent.InitialIntent> authInitIntent() {
        return Observable.just(Intent.InitialIntent.show());
    }

    private Observable<Intent.AuthenticationIntent> authenticationIntent() {
        return Observable.merge(
                clicks(authButton).map(ignored -> Intent.AuthenticationIntent.show()),
                cancels(authenticationDialog).map(ignored -> Intent.AuthenticationIntent.hide()));
    }

    private Observable<Intent.AuthActionIntent> authIntent() {
        return Observable.merge(
                authenticationDialog.positiveButtonClicks()
                        .filter(ignored ->
                                authenticationIdCardDataResponse != null &&
                                        authenticationIdCardDataResponse.token() != null)
                        .map(pin1 ->
                                Intent.AuthActionIntent.start(AuthRequest.create(
                                        authenticationIdCardDataResponse.token(), hash,
                                        pin1, sessionId, authenticationIdCardDataResponse.data().authCertificate(),
                                        context.getResources().openRawResource(ee.ria.DigiDoc.auth.R.raw.clientkeystore),
                                        context.getResources().openRawResource(R.raw.config)))),
                idCardTokenAvailableSubject
                        .filter(duplicates())
                        .filter(available -> available)
                        .map(ignored -> Intent.AuthActionIntent.cancel()));
    }

    @Override
    public void render(ViewState state) {
        if (state.hash() != null) {
            hash = state.hash();
        }

        authenticationError = state.authenticationError();

        if (state.authenticationSuccessMessageVisible()) {
            successMessage.setVisibility(View.VISIBLE);
        }
        authenticationIdCardDataResponse = state.authenticationIdCardDataResponse();
        signature = state.signature();
        boolean pin1Locked = false;
        if (authenticationIdCardDataResponse != null) {

            IdCardData data = authenticationIdCardDataResponse.data();
            if (data != null && data.pin1RetryCount() == 0) {
                pin1Locked = true;
            }
            authenticationDialog.show();
            authenticationDialog.idCardDataResponse(authenticationIdCardDataResponse, state.authenticationState(),
                    authenticationError);
        } else {
            pin1Locked = true;
            authenticationDialog.dismiss();
        }

        if (authenticationError != null) {
            if (authenticationError instanceof Pin1InvalidException && pin1Locked) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.auth_create_authenticate_pin1_locked));
                errorDialog.show();
            } else if (!(authenticationError instanceof Pin1InvalidException)) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.auth_create_error));
                errorDialog.show();
            }
        } else {
            errorDialog.dismiss();
        }
    }

    public Observable<Intent> intents() {
        return Observable.mergeArray(authInitIntent(), authIntent(), authenticationIntent());
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        this.context = context;
        viewModel = Application.component(context).navigator()
                .viewModel(getInstanceId(), AuthCreateViewModel.class);

    }

    @Override
    protected void onContextUnavailable() {
        viewModel = null;
        super.onContextUnavailable();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.auth_create_screen, container, false);
        authButton = view.findViewById(R.id.authHomeCreateButton);
        closeButton = view.findViewById(R.id.authHomeCloseButton);
        successMessage = view.findViewById(R.id.authCreateSuccessMessage);
        successMessage.setVisibility(View.GONE);

        authenticationDialog = new AuthenticationDialog(inflater.getContext(), VerificationCodeCalculator.calculate(hash.getBytes()));
        errorDialog = new AlertDialog.Builder(inflater.getContext())
                .setMessage(R.string.auth_create_error)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                .create();

        android.content.Intent mainActivity = new android.content.Intent(view.getContext(), Activity.class);
        disposables.attach();
        disposables.add(clicks(closeButton).subscribe(ignored ->
                startActivityForResult(mainActivity, 0)));
        tintCompoundDrawables(authButton, true);

        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        authenticationDialog.dismiss();
        errorDialog.dismiss();
        disposables.detach();
        super.onDestroyView(view);
    }

}