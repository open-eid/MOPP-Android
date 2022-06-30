package ee.ria.DigiDoc.android.main.home;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.main.about.AboutScreen;
import ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsScreen;
import ee.ria.DigiDoc.android.main.home.Intent.NavigationVisibilityIntent;
import ee.ria.DigiDoc.android.main.settings.SettingsScreen;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.signature.list.SignatureListScreen;
import ee.ria.DigiDoc.android.utils.LocaleService;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;

import static android.content.Intent.ACTION_VIEW;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createBrowserIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

final class Processor implements ObservableTransformer<Intent, Result> {

    private static final SparseIntArray NAVIGATION_ITEM_VIEWS = new SparseIntArray();
    static {
        NAVIGATION_ITEM_VIEWS.put(R.id.mainHomeNavigationSignature, R.id.mainHomeSignature);
        NAVIGATION_ITEM_VIEWS.put(R.id.mainHomeNavigationCrypto, R.id.mainHomeCrypto);
        NAVIGATION_ITEM_VIEWS.put(R.id.mainHomeNavigationEID, R.id.mainHomeEID);
    }

    private static final ImmutableBiMap<Integer, String> LOCALES =
            ImmutableBiMap.<Integer, String>builder()
                    .put(R.id.mainHomeMenuLocaleEt, "et")
                    .put(R.id.mainHomeMenuLocaleRu, "ru")
                    .put(R.id.mainHomeMenuLocaleEn, "en")
                    .build();

    private final Navigator navigator;

    private final ObservableTransformer<Intent.InitialIntent, Result.InitialResult> initial;

    private final ObservableTransformer<Intent.NavigationIntent,
                                        Result.NavigationResult> navigation;

    private final ObservableTransformer<Intent.MenuIntent, Result.MenuResult> menu;

    private final ObservableTransformer<NavigationVisibilityIntent,
                                        Result.NavigationVisibilityResult> navigationVisibility;

    private final ObservableTransformer<Intent.LocaleChangeIntent,
                                        Result.LocaleChangeResult> localeChange;

    @Nullable private String eidScreenId;

    @Inject Processor(Application application, Navigator navigator, LocaleService localeService,
                      ContentResolver contentResolver, FileSystem fileSystem) {
        this.navigator = navigator;

        initial = upstream -> upstream.switchMap(intent -> {
            if (intent.intent() != null
                    && TextUtils.equals(intent.intent().getAction(), ACTION_VIEW)
                    && intent.intent().getData() != null) {
                ImmutableList<FileStream> fileStreams =
                        parseGetContentIntent(contentResolver, intent.intent(), fileSystem.getExternallyOpenedFilesDir());
                Screen screen;
                if (fileStreams.size() == 1
                        && CryptoContainer.isContainerFileName(fileStreams.get(0).displayName())) {
                    screen = CryptoCreateScreen.open(intent.intent());
                } else {
                    screen = SignatureCreateScreen.create(intent.intent());
                }
                navigator.execute(Transaction.replace(screen));
                return Observable.never();
            }
            return Observable
                    .fromCallable(() -> Result.InitialResult.create(
                            R.id.mainHomeSignature,
                            LOCALES.inverse().get(
                                    localeService.applicationLocale().getLanguage())));
        })
                .onErrorReturn(throwable -> {
                    ToastUtil.showGeneralError(navigator.activity());
                    navigator.execute(Transaction.pop());
                    return Result.InitialResult.create(R.id.mainHomeSignature, null);
                });

        navigation = upstream -> upstream.switchMap(action -> {
            if (action.item() != R.id.mainHomeNavigationEID) {
                clearEidViewModel();
            } else {
                View myEidView = navigator.activity().findViewById(R.id.mainHomeNavigationEID);
                if (myEidView != null) {
                    myEidView.setContentDescription(application.getResources().getString(R.string.my_eid_content_description));
                }
            }
            return Observable.just(Result.NavigationResult
                    .create(NAVIGATION_ITEM_VIEWS.get(action.item())));
        });

        menu = upstream -> upstream.switchMap(action -> {
            Boolean isOpen = action.isOpen();
            Integer item = action.menuItem();
            if (isOpen != null) {
                return Observable.just(Result.MenuResult.create(isOpen));
            } else if (item != null) {
                clearEidViewModel();
                navigator.execute(menuItemToTransaction(application, item,
                        localeService.applicationConfigurationWithLocale(application.getApplicationContext(),
                                localeService.applicationLocale())));
                return Observable.just(Result.MenuResult.create(false));
            }
            throw new IllegalStateException("Action is in invalid state: " + action);
        });

        navigationVisibility = upstream -> upstream.map(action ->
                Result.NavigationVisibilityResult.create(action.visible()));

        localeChange = upstream -> upstream.switchMap(intent -> Observable
                .fromCallable(() -> {
                    localeService.applicationLocale(new Locale(LOCALES.get(intent.item())));
                    return Result.LocaleChangeResult.create(LOCALES.inverse().get(
                            localeService.applicationLocale().getLanguage()));
                })
                .doFinally(() -> AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.language_changed)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.NavigationIntent.class).compose(navigation),
                shared.ofType(Intent.MenuIntent.class).compose(menu),
                shared.ofType(NavigationVisibilityIntent.class).compose(navigationVisibility),
                shared.ofType(Intent.LocaleChangeIntent.class).compose(localeChange)));
    }

    void eidScreenId(@Nullable String eidScreenId) {
        this.eidScreenId = eidScreenId;
    }

    private void clearEidViewModel() {
        if (eidScreenId != null) {
            navigator.clearViewModel(eidScreenId);
        }
    }

    private static Transaction menuItemToTransaction(Context context, @IdRes int item, Configuration configuration) {
        switch (item) {
            case R.id.mainHomeMenuHelp:
                return Transaction
                        .activity(createBrowserIntent(context, R.string.main_home_menu_help_url, configuration),
                                null);
            case R.id.mainHomeMenuRecent:
                return Transaction.push(SignatureListScreen.create());
            case R.id.mainHomeMenuSettings:
                return Transaction.push(SettingsScreen.create());
            case R.id.mainHomeMenuAbout:
                return Transaction.push(AboutScreen.create());
            case R.id.mainHomeMenuDiagnostics:
                return Transaction.push(DiagnosticsScreen.create());
            default:
                throw new IllegalArgumentException("Unknown menu item: " + item);
        }
    }
}
