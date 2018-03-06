package ee.ria.DigiDoc.android.main.home;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class HomeViewModel extends BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject HomeViewModel(Processor processor) {
        super(processor);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Intent.InitialIntent) {
            return Action.NavigationAction.create(R.id.mainHomeNavigationSignature);
        } else if (intent instanceof Intent.NavigationIntent) {
            return Action.NavigationAction.create(((Intent.NavigationIntent) intent).item());
        } else if (intent instanceof Intent.MenuIntent) {
            Intent.MenuIntent menuIntent = (Intent.MenuIntent) intent;
            return Action.MenuAction.create(menuIntent.isOpen(), menuIntent.menuItem());
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
