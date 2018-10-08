package ee.ria.DigiDoc.android.auth;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class AuthCreateViewModel extends
            BaseMviViewModel<Intent, ViewState, Intent, Result> {

        @Inject
        AuthCreateViewModel(Processor processor) {
            super(processor);
        }

        @Override
        protected Class<? extends Intent> initialIntentType() {
            return Intent.InitialIntent.class;
        }

        @Override
        protected Intent action(Intent intent) {
            return intent;
        }

        @Override
        protected ViewState initialViewState() {
            return ViewState.initial();
        }
}

