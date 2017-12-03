package ee.ria.DigiDoc.android.utils.conductor;

import android.util.ArrayMap;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import ee.ria.DigiDoc.android.utils.mvi.MviViewModel;
import ee.ria.DigiDoc.android.utils.mvi.MviViewModelProvider;

/**
 * Try using android architecture components ViewModelProvider.
 */
public final class ConductorViewModelProvider implements MviViewModelProvider {

    private final Map<Class<? extends MviViewModel>, Provider<MviViewModel>> viewModelProviders;
    private final Map<Class<? extends MviViewModel>, MviViewModel> viewModels;

    @Inject
    ConductorViewModelProvider(
            Map<Class<? extends MviViewModel>, Provider<MviViewModel>> viewModelProviders) {
        this.viewModelProviders = viewModelProviders;
        viewModels = new ArrayMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends MviViewModel> T get(Class<T> type) {
        if (!viewModels.containsKey(type)) {
            viewModels.put(type, viewModelProviders.get(type).get());
        }
        return (T) viewModels.get(type);
    }
}
