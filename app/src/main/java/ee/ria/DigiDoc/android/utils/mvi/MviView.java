package ee.ria.DigiDoc.android.utils.mvi;

import io.reactivex.rxjava3.core.Observable;

public interface MviView<I extends MviIntent, S extends MviViewState> {

    Observable<I> intents();

    void render(S state);
}
