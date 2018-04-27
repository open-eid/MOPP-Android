package ee.ria.DigiDoc.android.main.about;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jakewharton.rxbinding2.support.v7.widget.RxToolbar;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class AboutView extends ScrollView {

    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    private TextView riaDigiDocVersionTitle;
    private TextView preferenceV7FixVersionTitle;
    private TextView guavaVersionTitle;
    private TextView commonsIoVersionTitle;
    private TextView okioVersionTitle;
    private TextView timberVersionTitle;
    private TextView butterknifeVersionTitle;
    private TextView okHttpVersionTitle;
    private TextView retrofitVersionTitle;
    private TextView spongyCastleVersionTitle;
    private TextView materialValuesVersionTitle;
    private TextView daggerVersionTitle;
    private TextView conductorVersionTitle;
    private TextView rxJavaVersionTitle;
    private TextView rxAndroidVersionTitle;
    private TextView rxBindingVersionTitle;
    private TextView autoValueVersionTitle;
    private TextView autoValueParcelVersionTitle;
    private TextView threeTenAbpVersionTitle;
    private TextView expandableLayoutVersionTitle;
    private TextView junitVersionTitle;
    private TextView truthVersionTitle;

    public AboutView(Context context) {
        super(context);
        inflate(context, R.layout.main_about, this);
        toolbarView = findViewById(R.id.toolbar);
        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();

        riaDigiDocVersionTitle = findViewById(R.id.mainAboutRiaDigiDocVersionTitle);
        preferenceV7FixVersionTitle =
                findViewById(R.id.mainAboutPreferenceV7FixVersionTitle);
        guavaVersionTitle = findViewById(R.id.mainAboutGuavaVersionTitle);
        commonsIoVersionTitle = findViewById(R.id.mainAboutCommonsIoVersionTitle);
        okioVersionTitle = findViewById(R.id.mainAboutOkioVersionTitle);
        timberVersionTitle = findViewById(R.id.mainAboutTimberVersionTitle);
        butterknifeVersionTitle = findViewById(R.id.mainAboutButterknifeVersionTitle);
        okHttpVersionTitle = findViewById(R.id.mainAboutOkHttpVersionTitle);
        retrofitVersionTitle = findViewById(R.id.mainAboutRetrofitVersionTitle);
        spongyCastleVersionTitle = findViewById(R.id.mainAboutSpongyCastleVersionTitle);
        materialValuesVersionTitle =
                findViewById(R.id.mainAboutMaterialValuesVersionTitle);
        daggerVersionTitle = findViewById(R.id.mainAboutDaggerVersionTitle);
        conductorVersionTitle = findViewById(R.id.mainAboutConductorVersionTitle);
        rxJavaVersionTitle = findViewById(R.id.mainAboutRxJavaVersionTitle);
        rxAndroidVersionTitle = findViewById(R.id.mainAboutRxAndroidVersionTitle);
        rxBindingVersionTitle = findViewById(R.id.mainAboutRxBindingVersionTitle);
        autoValueVersionTitle = findViewById(R.id.mainAboutAutoValueVersionTitle);
        autoValueParcelVersionTitle =
                findViewById(R.id.mainAboutAutoValueParcelVersionTitle);
        threeTenAbpVersionTitle = findViewById(R.id.mainAboutThreeTenAbpVersionTitle);
        expandableLayoutVersionTitle =
                findViewById(R.id.mainAboutExpandableLayoutVersionTitle);
        junitVersionTitle = findViewById(R.id.mainAboutJunitVersionTitle);
        truthVersionTitle = findViewById(R.id.mainAboutTruthVersionTitle);

        appendTitleVersions();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(RxToolbar.navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    private void appendTitleVersions() {
        riaDigiDocVersionTitle.append(" Version " + BuildConfig.VERSION_NAME);
        preferenceV7FixVersionTitle.append(" " + BuildConfig.PREFERENCE_V7_FIX_VERSION);
        guavaVersionTitle.append(" " + BuildConfig.GUAVA_VERSION);
        commonsIoVersionTitle.append(" " + BuildConfig.COMMONS_IO_VERSION);
        okioVersionTitle.append(" " + BuildConfig.OKIO_VERSION);
        timberVersionTitle.append(" " + BuildConfig.TIMBER_VERSION);
        butterknifeVersionTitle.append(" " + BuildConfig.BUTTERKNIFE_VERSION);
        okHttpVersionTitle.append(" " + BuildConfig.OK_HTTP_VERSION);
        retrofitVersionTitle.append(" " + BuildConfig.RETROFIT_VERSION);
        spongyCastleVersionTitle.append(" " + BuildConfig.SPONGY_CASTLE_VERSION);
        materialValuesVersionTitle.append(" " + BuildConfig.MATERIAL_VALUES_VERSION);
        daggerVersionTitle.append(" " + BuildConfig.DAGGER_VERSION);
        conductorVersionTitle.append(" " + BuildConfig.CONDUCTOR_VERSION);
        rxJavaVersionTitle.append(" " + BuildConfig.RX_JAVA_VERSION);
        rxAndroidVersionTitle.append(" " + BuildConfig.RX_ANDROID_VERSION);
        rxBindingVersionTitle.append(" " + BuildConfig.RX_BINDER_VERSION);
        autoValueVersionTitle.append(" " + BuildConfig.AUTO_VALUE_VERSION);
        autoValueParcelVersionTitle.append(" " + BuildConfig.AUTO_VALUE_PARCEL_VERSION);
        threeTenAbpVersionTitle.append(" " + BuildConfig.THREE_TEN_ABP_VERSION);
        expandableLayoutVersionTitle.append(" " + BuildConfig.EXPANDABLE_LAYOUT_VERSION);
        junitVersionTitle.append(" " + BuildConfig.JUNIT_VERSION);
        truthVersionTitle.append(" " + BuildConfig.TRUTH_VERSION);
    }
}
