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
import com.bluelinelabs.conductor.Controller;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.nio.ByteBuffer;

import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_INTENT;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;

public class AuthenticationCreateScreen extends Controller implements Screen,
        MviView<Intent, ViewState> {
    @Nullable
    private final android.content.Intent intent;
    private ByteBuffer signature;
    private String hash;
    private AuthenticationDialog authenticationDialog;
    private final Subject<Boolean> idCardTokenAvailableSubject = PublishSubject.create();
    private final ViewDisposables disposables = new ViewDisposables();
    private AuthCreateViewModel viewModel;
    private Button authButton;
    private AlertDialog errorDialog;
    private IdCardDataResponse authenticationIdCardDataResponse;
    @Nullable
    private Throwable authenticationError;

    public static AuthenticationCreateScreen create(String hash, String hashType, String sessionId) {
        Bundle args = new Bundle();
        args.putString(KEY_AUTHTOKEN, hash);
        return new AuthenticationCreateScreen(args);
    }

    public AuthenticationCreateScreen(Bundle args) {
        super(args);
        hash = args.getString(KEY_AUTHTOKEN);
        intent = args.getParcelable(KEY_INTENT);
    }

    private Observable<Intent.InitialIntent> authInitIntent() {
        return Observable.just(Intent.InitialIntent.show());
    }

    private Observable<Intent.AuthIntent> authIntent() {
        return Observable.merge(
                authenticationDialog.positiveButtonClicks()
                        .filter(ignored ->
                                authenticationIdCardDataResponse != null &&
                                        authenticationIdCardDataResponse.token() != null)
                        .map(pin1 ->
                                Intent.AuthIntent.start(AuthRequest.create(
                                        authenticationIdCardDataResponse.token(), hash,
                                        pin1))),
                idCardTokenAvailableSubject
                        .filter(duplicates())
                        .filter(available -> available)
                        .map(ignored -> Intent.AuthIntent.cancel()));
    }

    @Override
    public void render(ViewState state) {
        if (state.hash() != null) {
            hash = state.hash();
        }
        authenticationError = state.authenticationError();
        authenticationIdCardDataResponse = state.authenticationIdCardDataResponse();
        signature = state.signature();
        boolean decryptionPin1Locked = false;
        if (authenticationIdCardDataResponse != null) {
            IdCardData data = authenticationIdCardDataResponse.data();
            if (data != null && data.pin1RetryCount() == 0) {
                decryptionPin1Locked = true;
            }
            authenticationDialog.show();
            authenticationDialog.idCardDataResponse(authenticationIdCardDataResponse, state.authenticationState(),
                    authenticationError);
        } else {
            decryptionPin1Locked = true;
            authenticationDialog.dismiss();
        }

        if (authenticationError != null) {
            if (authenticationError instanceof Pin1InvalidException && decryptionPin1Locked) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_decrypt_pin1_locked));
                errorDialog.show();
            } else if (!(authenticationError instanceof Pin1InvalidException)) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_error));
                errorDialog.show();
            }
        } else {
            errorDialog.dismiss();
        }

    }

    public Observable<Intent> intents() {
        return Observable.mergeArray(authInitIntent(), authIntent());
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
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
        authButton = view.findViewById(R.id.cryptoHomeCreateButton);
//        navigator = Application.component(context).navigator();
        authenticationDialog = new AuthenticationDialog(inflater.getContext());
        errorDialog = new AlertDialog.Builder(inflater.getContext())
                .setMessage(R.string.auth_create_error)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                .create();
        disposables.attach();
//        disposables.add(clicks(authButton).subscribe(ignored ->
//                navigator.execute(Transaction.push(CryptoCreateScreen.create()))));
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        authenticationDialog.dismiss();
        disposables.detach();
        super.onDestroyView(view);
    }

}
