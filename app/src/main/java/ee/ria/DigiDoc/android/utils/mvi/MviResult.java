package ee.ria.DigiDoc.android.utils.mvi;

public interface MviResult<S extends MviViewState> {

    S reduce(S state);
}
