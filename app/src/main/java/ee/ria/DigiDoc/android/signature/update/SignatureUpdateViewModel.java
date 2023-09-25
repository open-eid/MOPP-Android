package ee.ria.DigiDoc.android.signature.update;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class SignatureUpdateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    private final SettingsDataStore settingsDataStore;

    @Inject SignatureUpdateViewModel(Processor processor, SettingsDataStore settingsDataStore) {
        super(processor);
        this.settingsDataStore = settingsDataStore;
    }

    public int signatureAddMethod() { return settingsDataStore.getSignatureAddMethod(); }

    public void setSignatureAddMethod(int method) { settingsDataStore.setSignatureAddMethod(method); }

    public String phoneNo() {
        return settingsDataStore.getPhoneNo();
    }

    public String country() {
        return settingsDataStore.getCountry();
    }

    public String personalCode() {
        return settingsDataStore.getPersonalCode();
    }

    public String sidPersonalCode() {
        return settingsDataStore.getSidPersonalCode();
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Action) {
            return (Action) intent;
        } else {
            return intent.action();
        }
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
