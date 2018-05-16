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

    private final TextView riaDigiDocVersionTitleView;
    private final TextView libDigidocppVersionTitleView;
    private final TextView xercesCVersionTitleView;
    private final TextView apacheXmlSecurityVersionTitleView;
    private final TextView xsdVersionTitleView;
    private final TextView opensslVersionTitleView;
    private final TextView libxmlVersionTitleView;
    private final TextView podofoVersionTitleView;
    private final TextView zLibVersionTitleView;
    private final TextView preferenceV7FixVersionTitleView;
    private final TextView guavaVersionTitleView;
    private final TextView okioVersionTitleView;
    private final TextView timberVersionTitleView;
    private final TextView okHttpVersionTitleView;
    private final TextView retrofitVersionTitleView;
    private final TextView spongyCastleVersionTitleView;
    private final TextView materialValuesVersionTitleView;
    private final TextView daggerVersionTitleView;
    private final TextView conductorVersionTitleView;
    private final TextView rxJavaVersionTitleView;
    private final TextView rxAndroidVersionTitleView;
    private final TextView rxBindingVersionTitleView;
    private final TextView autoValueVersionTitleView;
    private final TextView autoValueParcelVersionTitleView;
    private final TextView threeTenAbpVersionTitleView;
    private final TextView expandableLayoutVersionTitleView;
    private final TextView junitVersionTitleView;
    private final TextView truthVersionTitleView;

    public AboutView(Context context) {
        super(context);
        inflate(context, R.layout.main_about, this);
        toolbarView = findViewById(R.id.toolbar);
        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();

        riaDigiDocVersionTitleView = findViewById(R.id.mainAboutRiaDigiDocVersionTitle);
        libDigidocppVersionTitleView = findViewById(R.id.mainAboutLibdigidocppVersionTitle);
        xercesCVersionTitleView = findViewById(R.id.mainAboutXercesCVersionTitle);
        apacheXmlSecurityVersionTitleView =
                findViewById(R.id.mainAboutApacheXmlSecurityVersionTitle);
        xsdVersionTitleView = findViewById(R.id.mainAboutXsdVersionTitle);
        opensslVersionTitleView = findViewById(R.id.mainAboutOpensslVersionTitle);
        libxmlVersionTitleView = findViewById(R.id.mainAboutLibxmlVersionTitle);
        podofoVersionTitleView = findViewById(R.id.mainAboutPodofoVersionTitle);
        zLibVersionTitleView = findViewById(R.id.mainAboutZLibVersionTitle);
        preferenceV7FixVersionTitleView =
                findViewById(R.id.mainAboutPreferenceV7FixVersionTitle);
        guavaVersionTitleView = findViewById(R.id.mainAboutGuavaVersionTitle);
        okioVersionTitleView = findViewById(R.id.mainAboutOkioVersionTitle);
        timberVersionTitleView = findViewById(R.id.mainAboutTimberVersionTitle);
        okHttpVersionTitleView = findViewById(R.id.mainAboutOkHttpVersionTitle);
        retrofitVersionTitleView = findViewById(R.id.mainAboutRetrofitVersionTitle);
        spongyCastleVersionTitleView = findViewById(R.id.mainAboutSpongyCastleVersionTitle);
        materialValuesVersionTitleView =
                findViewById(R.id.mainAboutMaterialValuesVersionTitle);
        daggerVersionTitleView = findViewById(R.id.mainAboutDaggerVersionTitle);
        conductorVersionTitleView = findViewById(R.id.mainAboutConductorVersionTitle);
        rxJavaVersionTitleView = findViewById(R.id.mainAboutRxJavaVersionTitle);
        rxAndroidVersionTitleView = findViewById(R.id.mainAboutRxAndroidVersionTitle);
        rxBindingVersionTitleView = findViewById(R.id.mainAboutRxBindingVersionTitle);
        autoValueVersionTitleView = findViewById(R.id.mainAboutAutoValueVersionTitle);
        autoValueParcelVersionTitleView =
                findViewById(R.id.mainAboutAutoValueParcelVersionTitle);
        threeTenAbpVersionTitleView = findViewById(R.id.mainAboutThreeTenAbpVersionTitle);
        expandableLayoutVersionTitleView =
                findViewById(R.id.mainAboutExpandableLayoutVersionTitle);
        junitVersionTitleView = findViewById(R.id.mainAboutJunitVersionTitle);
        truthVersionTitleView = findViewById(R.id.mainAboutTruthVersionTitle);

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
        riaDigiDocVersionTitleView.setText(getResources().getString(
                R.string.main_about_ria_digidoc_version_title, BuildConfig.VERSION_NAME));
        libDigidocppVersionTitleView.setText(getResources().getString(
                R.string.main_about_libdigidocpp_title, digidoc.version()));
        xercesCVersionTitleView.setText(getResources().getString(
                R.string.main_about_xerces_c_title, "3.2.1"));
        apacheXmlSecurityVersionTitleView.setText(getResources().getString(
                R.string.main_about_apache_xml_security_title, "1.7.3"));
        xsdVersionTitleView.setText(getResources().getString(
                R.string.main_about_xsd_title, "4.0.0"));
        opensslVersionTitleView.setText(getResources().getString(
                R.string.main_about_openssl_title, "1.0.2o"));
        libxmlVersionTitleView.setText(getResources().getString(
                R.string.main_about_libxml_title, "2-2.9.8"));
        podofoVersionTitleView.setText(getResources().getString(
                R.string.main_about_podofo_title, "0.9.4"));
        zLibVersionTitleView.setText(getResources().getString(
                R.string.main_about_z_lib_title, "1.2.11"));
        preferenceV7FixVersionTitleView.setText(getResources().getString(
                R.string.main_about_support_preference_v7_fix_title,
                BuildConfig.PREFERENCE_V7_FIX_VERSION));
        guavaVersionTitleView.setText(getResources().getString(
                R.string.main_about_guava_title, BuildConfig.GUAVA_VERSION));
        okioVersionTitleView.setText(getResources().getString(
                R.string.main_about_square_okio_title, BuildConfig.OKIO_VERSION));
        timberVersionTitleView.setText(getResources().getString(
                R.string.main_about_timber_title, BuildConfig.TIMBER_VERSION));
        okHttpVersionTitleView.setText(getResources().getString(
                R.string.main_about_square_okhttp_title, BuildConfig.OK_HTTP_VERSION));
        retrofitVersionTitleView.setText(getResources().getString(
                R.string.main_about_retrofit_title, BuildConfig.RETROFIT_VERSION));
        spongyCastleVersionTitleView.setText(getResources().getString(
                R.string.main_about_spongy_castle_title, BuildConfig.SPONGY_CASTLE_VERSION));
        materialValuesVersionTitleView.setText(getResources().getString(
                R.string.main_about_material_values_title, BuildConfig.MATERIAL_VALUES_VERSION));
        daggerVersionTitleView.setText(getResources().getString(
                R.string.main_about_dagger_title, BuildConfig.DAGGER_VERSION));
        conductorVersionTitleView.setText(getResources().getString(
                R.string.main_about_conductor_title, BuildConfig.CONDUCTOR_VERSION));
        rxJavaVersionTitleView.setText(getResources().getString(
                R.string.main_about_rxjava_title, BuildConfig.RX_JAVA_VERSION));
        rxAndroidVersionTitleView.setText(getResources().getString(
                R.string.main_about_rxandroid_title, BuildConfig.RX_ANDROID_VERSION));
        rxBindingVersionTitleView.setText(getResources().getString(
                R.string.main_about_rxbinding_title, BuildConfig.RX_BINDER_VERSION));
        autoValueVersionTitleView.setText(getResources().getString(
                R.string.main_about_autovalue_title, BuildConfig.AUTO_VALUE_VERSION));
        autoValueParcelVersionTitleView.setText(getResources().getString(
                R.string.main_about_autovalue_parcel_title, BuildConfig.AUTO_VALUE_PARCEL_VERSION));
        threeTenAbpVersionTitleView.setText(getResources().getString(
                R.string.main_about_threetenabp_title, BuildConfig.THREE_TEN_ABP_VERSION));
        expandableLayoutVersionTitleView.setText(getResources().getString(
                R.string.main_about_expandable_layout_title,
                BuildConfig.EXPANDABLE_LAYOUT_VERSION));
        junitVersionTitleView.setText(getResources().getString(
                R.string.main_about_junit_title, BuildConfig.JUNIT_VERSION));
        truthVersionTitleView.setText(getResources().getString(
                R.string.main_about_truth_title, BuildConfig.TRUTH_VERSION));
    }
}
