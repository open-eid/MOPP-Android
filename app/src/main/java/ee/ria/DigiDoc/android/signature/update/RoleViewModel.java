package ee.ria.DigiDoc.android.signature.update;

import java.util.List;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.common.RoleData;

public final class RoleViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    private final SettingsDataStore settingsDataStore;

    @Inject
    RoleViewModel(Processor processor, SettingsDataStore settingsDataStore) {
        super(processor);
        this.settingsDataStore = settingsDataStore;
    }

    public List<String> roles() {
        return settingsDataStore.getRoles();
    }

    public String city() {
        return settingsDataStore.getRoleCity();
    }

    public String state() {
        return settingsDataStore.getRoleState();
    }

    public String country() {
        return settingsDataStore.getRoleCountry();
    }

    public String zip() {
        return settingsDataStore.getRoleZip();
    }

    void setRoleData(RoleData roleData) {
        settingsDataStore.setRoles(roleData.getRoles());
        settingsDataStore.setRoleCity(roleData.getCity());
        settingsDataStore.setRoleState(roleData.getState());
        settingsDataStore.setRoleCountry(roleData.getCountry());
        settingsDataStore.setRoleZip(roleData.getZip());
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        return (Action) intent;
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}