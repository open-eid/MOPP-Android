package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import java.util.Locale;
import java.util.Set;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.common.TextUtil;
import io.reactivex.rxjava3.core.Observable;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxRadioGroup.checkedChanges;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;

public final class HomeMenuView extends NestedScrollView {

    private final View closeButton;

    private final Button helpView;
    private final Button recentView;
    private final Button settingsView;
    private final Button aboutView;
    private final Button diagnosticsView;
    private final RadioGroup localeView;

    // Estonian TalkBack does not pronounce "dot"
    private final TextToSpeech textToSpeech = new TextToSpeech(getContext(),
            new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            Voice textToSpeechVoice = textToSpeech.getVoice();
            String language = Locale.getDefault().getLanguage();
            boolean isESTLanguageAvailable = isTextToSpeechLanguageAvailable(textToSpeech.getAvailableLanguages(),
                    Set.of(new Locale("est", "EST"), new Locale("et", "ET")));
            if ((textToSpeechVoice == null && isESTLanguageAvailable) ||
                    (textToSpeechVoice != null &&
                            textToSpeechVoice.getLocale().getLanguage().equals("et"))) {
                language = "et";
            }
            if (language.equals("et")) {
                helpView.setContentDescription(
                        getResources().getString(R.string.main_home_menu_help) +
                                " link " +
                                "w w w punkt i d punkt e e");
            } else {
                helpView.setContentDescription(
                        getResources().getString(R.string.main_home_menu_help) + " " +
                                TextUtil.splitTextAndJoin(
                                        getResources().getString(R.string.main_home_menu_help_url_short), "", " "));
            }
        }
    });

    public HomeMenuView(@NonNull Context context) {
        this(context, null);
    }

    public HomeMenuView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeMenuView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_home_menu, this);
        closeButton = findViewById(R.id.mainHomeMenuCloseButton);
        closeButton.setContentDescription(getResources().getString(R.string.close_menu));
        helpView = findViewById(R.id.mainHomeMenuHelp);
        helpView.setContentDescription(
                getResources().getString(R.string.main_home_menu_help) + " " +
                        TextUtil.splitTextAndJoin(
                                getResources().getString(R.string.main_home_menu_help_url_short), "", " "));
        recentView = findViewById(R.id.mainHomeMenuRecent);
        settingsView = findViewById(R.id.mainHomeMenuSettings);
        aboutView = findViewById(R.id.mainHomeMenuAbout);
        diagnosticsView = findViewById(R.id.mainHomeMenuDiagnostics);
        localeView = findViewById(R.id.mainHomeMenuLocale);

        tintCompoundDrawables(helpView);
        tintCompoundDrawables(recentView);
        tintCompoundDrawables(settingsView);
        tintCompoundDrawables(aboutView);
        tintCompoundDrawables(diagnosticsView);

        helpView.postDelayed(() -> {
            helpView.requestFocus();
            helpView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 2000);
    }

    public Observable closeButtonClicks() {
        return clicks(closeButton);
    }

    @SuppressWarnings("unchecked")
    public Observable<Integer> itemClicks() {
        return Observable.mergeArray(
                clicks(helpView).map(ignored -> R.id.mainHomeMenuHelp),
                clicks(recentView).map(ignored -> R.id.mainHomeMenuRecent),
                clicks(settingsView).map(ignored -> R.id.mainHomeMenuSettings),
                clicks(aboutView).map(ignored -> R.id.mainHomeMenuAbout),
                clicks(diagnosticsView).map(ignored -> R.id.mainHomeMenuDiagnostics));
    }

    public void locale(@Nullable Integer locale) {
        if (locale == null) {
            localeView.clearCheck();
        } else {
            localeView.check(locale);
        }
    }

    public Observable<Integer> localeChecks() {
        return checkedChanges(localeView).skipInitialValue();
    }

    private boolean isTextToSpeechLanguageAvailable(Set<Locale> availableLocales, Set<Locale> locales) {
        return locales.stream().anyMatch(lo ->
                availableLocales.stream().anyMatch(b -> b.getLanguage().equals(lo.getLanguage()))
        );
    }
}
