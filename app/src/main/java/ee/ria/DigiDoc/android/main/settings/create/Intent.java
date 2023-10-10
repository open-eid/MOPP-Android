package ee.ria.DigiDoc.android.main.settings.create;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

public interface Intent extends MviIntent {
    Action action();
}

class InitialIntent implements Intent {

    public static InitialIntent clear() {
        return null;
    }

    static InitialIntent create() {
        return new InitialIntent();
    }

    @Override
    public Action action() {
        return Action.InitialAction.create();
    }
}

@AutoValue
class ChooseTSAFileIntent implements Intent {

    public static ChooseTSAFileIntent clear() {
        return null;
    }

    static ChooseTSAFileIntent create() {
        return new ChooseTSAFileIntent();
    }

    @Override
    public Action action() {
        return Action.ChooseTSAFileAction.create();
    }
}

@AutoValue
class ChooseSivaFileIntent implements Intent {

    public static ChooseSivaFileIntent clear() {
        return null;
    }

    static ChooseSivaFileIntent create() {
        return new ChooseSivaFileIntent();
    }

    @Override
    public Action action() {
        return Action.ChooseSivaFileAction.create();
    }
}
