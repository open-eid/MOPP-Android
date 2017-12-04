package ee.ria.DigiDoc.android.signature.update;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFiles;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadContainerAction,
                                        Result.LoadContainerResult> loadContainer =
            upstream -> upstream.flatMap(action -> loadContainer(action.containerFile())
                    .toObservable()
                    .map(Result.LoadContainerResult::success)
                    .onErrorReturn(AutoValue_Result_LoadContainerResult::failure)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWith(Result.LoadContainerResult.progress()));

    @Inject
    Processor() {
    }

    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.merge(
                shared.ofType(Action.LoadContainerAction.class).compose(loadContainer),
                Observable.empty()));
    }

    private Single<SignatureContainer> loadContainer(File containerFile) {
        return Single.fromCallable(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }

            ImmutableList.Builder<Document> documentBuilder = ImmutableList.builder();
            DataFiles dataFiles = container.dataFiles();
            for (int i = 0; i < dataFiles.size(); i++) {
                documentBuilder.add(Document.create(dataFiles.get(i).fileName()));
            }

            return SignatureContainer.create(containerFile.getName(), documentBuilder.build());
        });
    }
}
