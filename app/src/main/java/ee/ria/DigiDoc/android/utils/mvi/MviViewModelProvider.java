package ee.ria.DigiDoc.android.utils.mvi;

public interface MviViewModelProvider {

    <T extends MviViewModel> T get(Class<T> type);
}
