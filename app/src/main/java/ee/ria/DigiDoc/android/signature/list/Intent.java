package ee.ria.DigiDoc.android.signature.list;

import android.content.Context;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.SivaUtil;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.mvi.MviIntent;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

interface Intent extends MviIntent {
    Action action();
}

class InitialIntent implements Intent {
    private InitialIntent() {}

    static InitialIntent create() {
        return new InitialIntent();
    }

    @Override
    public Action action() {
        return Action.ContainersLoadAction.create(true);
    }
}

class UpButtonIntent implements Intent {
    private UpButtonIntent() {}

    static UpButtonIntent create() {
        return new UpButtonIntent();
    }

    @Override
    public Action action() {
        return Action.NavigateUpAction.create();
    }
}

class ContainerOpenIntent implements Intent {
    private final File containerFile;
    private final boolean confirmation;
    private final boolean isSivaConfirmed;

    private ContainerOpenIntent(File containerFile, boolean confirmation, boolean isSivaConfirmed) {
        this.containerFile = containerFile;
        this.confirmation = confirmation;
        this.isSivaConfirmed = isSivaConfirmed;
    }

    static Observable<ContainerOpenIntent> confirmation(File containerFile, Context context) {
        return SivaUtil.isSivaConfirmationNeeded(
                        ImmutableList.of(FileStream.create(containerFile)), context)
                .observeOn(AndroidSchedulers.mainThread())
                .map(isSivaConfirmationNeeded -> new ContainerOpenIntent(containerFile, isSivaConfirmationNeeded, false))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    static ContainerOpenIntent open(File containerFile, boolean isSivaConfirmed) {
        return new ContainerOpenIntent(containerFile, false, isSivaConfirmed);
    }

    static ContainerOpenIntent cancel() {
        return new ContainerOpenIntent(null, false, true);
    }

    @Override
    public Action action() {
        return Action.ContainerOpenAction.create(containerFile, confirmation, isSivaConfirmed);
    }
}

class ContainerRemoveIntent implements Intent {
    private final File containerFile;
    private final boolean confirmation;

    private ContainerRemoveIntent(File containerFile, boolean confirmation) {
        this.containerFile = containerFile;
        this.confirmation = confirmation;
    }

    static ContainerRemoveIntent confirmation(File containerFile) {
        return new ContainerRemoveIntent(containerFile, true);
    }

    static ContainerRemoveIntent remove(File containerFile) {
        return new ContainerRemoveIntent(containerFile, false);
    }

    static ContainerRemoveIntent cancel() {
        return new ContainerRemoveIntent(null, false);
    }

    @Override
    public Action action() {
        return Action.ContainerRemoveAction.create(containerFile, confirmation);
    }
}

class RefreshIntent implements Intent {
    private RefreshIntent() {}

    static RefreshIntent create() {
        return new RefreshIntent();
    }

    @Override
    public Action action() {
        return Action.ContainersLoadAction.create(false);
    }
}
