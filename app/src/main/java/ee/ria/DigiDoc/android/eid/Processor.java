package ee.ria.DigiDoc.android.eid;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.view.accessibility.AccessibilityEvent;

import com.google.common.collect.ImmutableSet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeInvalidError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeMinLengthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfDateOfBirthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfPersonalCodeError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeSameAsCurrentError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeTooEasyError;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.utils.LocaleService;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.idcard.CodeType;
import ee.ria.DigiDoc.idcard.CodeVerificationException;
import ee.ria.DigiDoc.idcard.Token;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static ee.ria.DigiDoc.android.utils.IntentUtils.createBrowserIntent;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadAction, Result.LoadResult> load;

    private final ObservableTransformer<Action.CertificatesTitleClickAction,
                                        Result.CertificatesTitleClickResult> certificatesTitleClick;

    private final ObservableTransformer<Intent.CodeUpdateIntent, Result.CodeUpdateResult>
            codeUpdate;

    @Inject Processor(Application application, Navigator navigator, IdCardService idCardService, LocaleService localeService) {
        load = upstream -> upstream.switchMap(action -> {
            Observable<Result.LoadResult> resultObservable = idCardService.data()
                    .map(idCardDataResponse -> {
                        if (idCardDataResponse.error() != null) {
                            return Result.LoadResult.failure(idCardDataResponse.error());
                        } else {
                            return Result.LoadResult.success(idCardDataResponse);
                        }
                    })
                    .onErrorReturn(Result.LoadResult::failure);
            if (action.clear()) {
                return resultObservable
                        .startWithItem(Result.LoadResult.clear());
            }
            return resultObservable;
        });

        certificatesTitleClick = upstream -> upstream.map(action ->
                Result.CertificatesTitleClickResult.create(action.expand()));

        codeUpdate = upstream -> upstream.flatMap(action -> {
            CodeUpdateAction updateAction = action.action();
            CodeUpdateRequest request = action.request();
            IdCardData data = action.data();
            Token token = action.token();
            if (action.cleared()) {
                return Observable.just(Result.CodeUpdateResult.clear())
                        .doFinally(() -> sendCancellationAccessibilityEvent(updateAction, application,
                                localeService.applicationConfigurationWithLocale(application.getApplicationContext(),
                                        localeService.applicationLocale())));
            } else if (updateAction == null) {
                return Observable.just(Result.CodeUpdateResult.clear());
            } else if (request == null || data == null || token == null) {
                if (updateAction.pinType().equals(CodeType.PUK)
                        && updateAction.updateType().equals(CodeUpdateType.UNBLOCK)) {
                    navigator.execute(Transaction
                            .activity(createBrowserIntent(application,
                                    R.string.eid_home_data_certificates_puk_link_url,
                                    localeService.applicationConfigurationWithLocale(application.getApplicationContext(),
                                            localeService.applicationLocale())), null));
                    return Observable.just(Result.CodeUpdateResult.clear());
                } else {
                    return Observable.just(Result.CodeUpdateResult.action(updateAction));
                }
            } else {
                CodeUpdateResponse response = validate(updateAction, request, data);
                if (!response.success()) {
                    return Observable.just(Result.CodeUpdateResult
                            .response(updateAction, response, null, null));
                }

                Single<IdCardData> operation;
                if (updateAction.updateType().equals(CodeUpdateType.EDIT)) {
                    operation = idCardService
                            .editPin(token, updateAction.pinType(), request.currentValue(),
                                    request.newValue());
                } else {
                    operation = idCardService
                            .unblockPin(token, updateAction.pinType(), request.currentValue(),
                                    request.newValue());
                }
                return operation
                        .toObservable()
                        .flatMap(idCardData ->
                                Observable
                                        .timer(3, TimeUnit.SECONDS)
                                        .map(ignored ->
                                                Result.CodeUpdateResult
                                                        .hideSuccessResponse(updateAction,
                                                                CodeUpdateResponse.valid(),
                                                                idCardData, token))
                                        .startWithArray(
                                                Result.CodeUpdateResult
                                                        .clearResponse(updateAction,
                                                                CodeUpdateResponse.valid(),
                                                                idCardData, token),
                                                Result.CodeUpdateResult
                                                        .successResponse(updateAction,
                                                                CodeUpdateResponse.valid(),
                                                                idCardData, token)))
                        .onErrorReturn(throwable -> {
                            IdCardData idCardData = IdCardService.data(token);
                            int retryCount = retryCount(updateAction, idCardData);

                            CodeUpdateResponse.Builder builder = CodeUpdateResponse.valid()
                                    .buildWith();
                            if (throwable instanceof CodeVerificationException && retryCount > 0) {
                                builder.currentError(CodeInvalidError.create(retryCount));
                            } else {
                                builder.error(throwable);
                            }

                            return Result.CodeUpdateResult
                                    .response(updateAction, builder.build(), idCardData, token);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.CodeUpdateResult.progress(updateAction));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.LoadAction.class).compose(load),
                shared.ofType(Action.CertificatesTitleClickAction.class)
                        .compose(certificatesTitleClick),
                shared.ofType(Intent.CodeUpdateIntent.class).compose(codeUpdate)));
    }

    private static CodeUpdateResponse validate(CodeUpdateAction action, CodeUpdateRequest request,
                                               IdCardData data) {
        LocalDate dateOfBirth = data.personalData().dateOfBirth();
        ImmutableSet.Builder<String> dateOfBirthValuesBuilder = ImmutableSet.builder();
        if (dateOfBirth != null) {
            dateOfBirthValuesBuilder
                    .add(dateOfBirth.format(DateTimeFormatter.ofPattern("yyyy")))
                    .add(dateOfBirth.format(DateTimeFormatter.ofPattern("MMdd")))
                    .add(dateOfBirth.format(DateTimeFormatter.ofPattern("ddMM")));
        }
        ImmutableSet<String> dateOfBirthValues = dateOfBirthValuesBuilder.build();

        CodeUpdateResponse.Builder builder = CodeUpdateResponse.valid().buildWith();

        // current
        if (request.currentValue().length() < action.currentMinLength()) {
            builder.currentError(CodeMinLengthError.create(action.currentMinLength()));
        }

        // new
        if (request.newValue().length() < action.newMinLength()) {
            builder.newError(CodeMinLengthError.create(action.newMinLength()));
        } else if (action.updateType().equals(CodeUpdateType.EDIT)
                && request.newValue().equals(request.currentValue())) {
            builder.newError(CodeSameAsCurrentError.create());
        } else if (data.personalData().personalCode().contains(request.newValue())) {
            builder.newError(CodePartOfPersonalCodeError.create());
        } else if (dateOfBirthValues.contains(request.newValue())) {
            builder.newError(CodePartOfDateOfBirthError.create());
        } else if (isCodeTooEasy(request.newValue())) {
            builder.newError(CodeTooEasyError.create());
        }

        // repeat
        if (!request.newValue().equals(request.repeatValue())) {
            builder.repeatError(CodeUpdateError.CodeRepeatMismatchError.create());
        }

        return builder.build();
    }

    /**
     * Checks that the code doesn't contain only one number nor growing or shrinking by one.
     *
     * Examples: 00000, 5555, 1234, 98765.
     *
     * @param code Code to check.
     * @return True if the code is too easy.
     */
    private static boolean isCodeTooEasy(String code) {
        Integer delta = null;
        for (int i = 0; i < code.length() - 1; i++) {
            int d = Character.getNumericValue(code.charAt(i)) -
                    Character.getNumericValue(code.charAt(i + 1));
            if (Math.abs(d) > 1) {
                return false;
            }
            if (delta != null && delta != d) {
                return false;
            }
            delta = d;
        }
        return true;
    }

    private int retryCount(CodeUpdateAction action, IdCardData data) {
        CodeType pinType = action.pinType();
        String updateType = action.updateType();
        if (updateType.equals(CodeUpdateType.UNBLOCK) || pinType.equals(CodeType.PUK)) {
            return data.pukRetryCount();
        } else if (pinType.equals(CodeType.PIN1)) {
            return data.pin1RetryCount();
        } else {
            return data.pin2RetryCount();
        }
    }

    private void sendCancellationAccessibilityEvent(CodeUpdateAction updateAction, Application application, Configuration configuration) {
        String actionText = "";
        Context configurationContext = application.getApplicationContext().createConfigurationContext(configuration);
        switch (updateAction.pinType()) {
            case PIN1:
                if (updateAction.updateType().equals(CodeUpdateType.UNBLOCK)) {
                    actionText = configurationContext.getText(application.getResources()
                                    .getIdentifier("pin1_unblock_cancelled", "string",
                                            application.getPackageName())).toString();
                } else {
                    actionText = configurationContext.getText(application.getResources()
                            .getIdentifier("pin1_change_cancelled", "string",
                                    application.getPackageName())).toString();
                }
                break;
            case PIN2:
                if (updateAction.updateType().equals(CodeUpdateType.UNBLOCK)) {
                    actionText = configurationContext.getText(application.getResources()
                            .getIdentifier("pin2_unblock_cancelled", "string",
                                    application.getPackageName())).toString();
                } else {
                    actionText = configurationContext.getText(application.getResources()
                            .getIdentifier("pin2_change_cancelled", "string",
                                    application.getPackageName())).toString();
                }
                break;
            case PUK:
                actionText = configurationContext.getText(application.getResources()
                        .getIdentifier("puk_code_change_cancelled", "string",
                                application.getPackageName())).toString();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + updateAction.pinType());
        }
        AccessibilityUtils.sendAccessibilityEvent(
                application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, actionText);
    }
}
