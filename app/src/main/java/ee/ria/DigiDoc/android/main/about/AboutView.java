package ee.ria.DigiDoc.android.main.about;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.libdigidocpp.digidoc;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class AboutView extends CoordinatorLayout {

    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    private TextView libDigidocppVersionTitle;
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

        libDigidocppVersionTitle = findViewById(R.id.mainAboutLibdigidocppVersionTitle);
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
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    private void appendTitleVersions() {
        riaDigiDocVersionTitle.setText(getResources().getString(
                R.string.main_about_ria_digidoc_version_title, BuildConfig.VERSION_NAME));
        libDigidocppVersionTitle.setText(getResources().getString(
                R.string.main_about_libdigidocpp_title, digidoc.version()));
        preferenceV7FixVersionTitle.setText(getResources().getString(
                R.string.main_about_support_preference_v7_fix_title,
                BuildConfig.PREFERENCE_V7_FIX_VERSION));
        guavaVersionTitle.setText(getResources().getString(
                R.string.main_about_guava_title, BuildConfig.GUAVA_VERSION));
        commonsIoVersionTitle.setText(getResources().getString(
                R.string.main_about_apache_commons_io_title, BuildConfig.COMMONS_IO_VERSION));
        okioVersionTitle.setText(getResources().getString(
                R.string.main_about_square_okio_title, BuildConfig.OKIO_VERSION));
        timberVersionTitle.setText(getResources().getString(
                R.string.main_about_timber_title, BuildConfig.TIMBER_VERSION));
        butterknifeVersionTitle.setText(getResources().getString(
                R.string.main_about_butterknife_title, BuildConfig.BUTTERKNIFE_VERSION));
        okHttpVersionTitle.setText(getResources().getString(
                R.string.main_about_square_okhttp_title, BuildConfig.OK_HTTP_VERSION));
        retrofitVersionTitle.setText(getResources().getString(
                R.string.main_about_retrofit_title, BuildConfig.RETROFIT_VERSION));
        spongyCastleVersionTitle.setText(getResources().getString(
                R.string.main_about_spongy_castle_title, BuildConfig.SPONGY_CASTLE_VERSION));
        materialValuesVersionTitle.setText(getResources().getString(
                R.string.main_about_material_values_title, BuildConfig.MATERIAL_VALUES_VERSION));
        daggerVersionTitle.setText(getResources().getString(
                R.string.main_about_dagger_title, BuildConfig.DAGGER_VERSION));
        conductorVersionTitle.setText(getResources().getString(
                R.string.main_about_conductor_title, BuildConfig.CONDUCTOR_VERSION));
        rxJavaVersionTitle.setText(getResources().getString(
                R.string.main_about_rxjava_title, BuildConfig.RX_JAVA_VERSION));
        rxAndroidVersionTitle.setText(getResources().getString(
                R.string.main_about_rxandroid_title, BuildConfig.RX_ANDROID_VERSION));
        rxBindingVersionTitle.setText(getResources().getString(
                R.string.main_about_rxbinding_title, BuildConfig.RX_BINDER_VERSION));
        autoValueVersionTitle.setText(getResources().getString(
                R.string.main_about_autovalue_title, BuildConfig.AUTO_VALUE_VERSION));
        autoValueParcelVersionTitle.setText(getResources().getString(
                R.string.main_about_autovalue_parcel_title, BuildConfig.AUTO_VALUE_PARCEL_VERSION));
        threeTenAbpVersionTitle.setText(getResources().getString(
                R.string.main_about_threetenabp_title, BuildConfig.THREE_TEN_ABP_VERSION));
        expandableLayoutVersionTitle.setText(getResources().getString(
                R.string.main_about_expandable_layout_title,
                BuildConfig.EXPANDABLE_LAYOUT_VERSION));
        junitVersionTitle.setText(getResources().getString(
                R.string.main_about_junit_title, BuildConfig.JUNIT_VERSION));
        truthVersionTitle.setText(getResources().getString(
                R.string.main_about_truth_title, BuildConfig.TRUTH_VERSION));
    }
}
