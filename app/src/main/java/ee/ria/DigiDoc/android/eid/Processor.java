package ee.ria.DigiDoc.android.eid;

import com.google.common.collect.ImmutableSet;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeMinLengthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfDateOfBirthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfPersonalCodeError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeTooEasyError;
import ee.ria.DigiDoc.android.model.EIDData;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadAction, Result.LoadResult> load;

    private final ObservableTransformer<Action.CertificatesTitleClickAction,
                                        Result.CertificatesTitleClickResult> certificatesTitleClick;

    private final ObservableTransformer<Intent.CodeUpdateIntent, Result.CodeUpdateResult>
            codeUpdate;

    @Inject Processor(IdCardService idCardService) {
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
                        .startWith(Result.LoadResult.clear());
            }
            return resultObservable;
        });

        certificatesTitleClick = upstream -> upstream.map(action ->
                Result.CertificatesTitleClickResult.create(action.expand()));

        codeUpdate = upstream -> upstream.flatMap(action -> {
            CodeUpdateAction updateAction = action.action();
            CodeUpdateRequest request = action.request();
            EIDData data = action.data();
            if (updateAction == null) {
                return Observable.just(Result.CodeUpdateResult.clear());
            } else if (request == null || data == null) {
                return Observable.just(Result.CodeUpdateResult.action(updateAction));
            } else {
                CodeUpdateResponse response = validate(updateAction, request, data);
                if (!response.success()) {
                    return Observable.just(Result.CodeUpdateResult
                            .response(updateAction, response));
                }
                return Observable.just(Result.CodeUpdateResult.progress(updateAction));
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
                                               EIDData data) {
        LocalDate dateOfBirth = data.dateOfBirth();
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
        } else if (data.personalCode().contains(request.newValue())) {
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
}
