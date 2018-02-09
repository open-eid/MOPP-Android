package ee.ria.DigiDoc.android.signature.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;

public final class SignatureCreateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    private final Navigator navigator;
//    private final Disposable activityResultsDisposable;

    @Inject SignatureCreateViewModel(Processor processor, Navigator navigator) {
        super(processor);
        this.navigator = navigator;
//        activityResultsDisposable = navigator.activityResults(RC_SIGNATURE_CREATE_DOCUMENTS_ADD)
//                .subscribe(activityResult -> {
//                    if (activityResult.resultCode() == RESULT_OK) {
//                        Timber.e("RESULT IS OK");
//                        intentSubject.onNext(Intent.CreateContainerIntent.create(
//                                parseGetContentIntent(navigator.context().getContentResolver(),
//                                        activityResult.data())));
//                    } else {
//                        Timber.e("RESULT IS POP");
//                        navigator.execute(Transaction.pop());
//                    }
//                });
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

//    @Override
//    protected boolean filterIntent(Intent intent) {
//        if (intent instanceof Intent.InitialIntent) {
//            navigator.getActivityResult(RC_SIGNATURE_CREATE_DOCUMENTS_ADD,
//                    createGetContentIntent());
//            return false;
//        }
//        return true;
//    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Intent.CreateContainerIntent) {
            return Action.CreateContainerAction
                    .create(((Intent.CreateContainerIntent) intent).fileStreams());
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }

//    @Override
//    protected void onResult(Result result) {
//        if (result instanceof Result.CreateContainerResult
//                && ((Result.CreateContainerResult) result).error() != null) {
//            Toast.makeText(navigator.context(), R.string.signature_create_error, Toast.LENGTH_LONG)
//                    .show();
//        }
//    }

//    @Override
//    protected void onDispose() {
//        activityResultsDisposable.dispose();
//    }
}
