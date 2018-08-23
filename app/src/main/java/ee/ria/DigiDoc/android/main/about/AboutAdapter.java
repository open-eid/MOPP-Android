package ee.ria.DigiDoc.android.main.about;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.sign.SignLib;

final class AboutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ImmutableList<Component> components = ImmutableList.<Component>builder()
            .add(Component.create(
                    R.string.main_about_libdigidocpp_title,
                    SignLib.libdigidocppVersion(),
                    R.string.main_about_lgpl_2_1_license_title,
                    R.string.main_about_lgpl_2_1_license_url))
            .add(Component.create(
                    R.string.main_about_xerces_c_title,
                    "3.2.1",
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_apache_xml_security_title,
                    "1.7.3",
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_xsd_title,
                    "4.0.0",
                    R.string.main_about_gpl_license_title,
                    R.string.main_about_xsd_license_url))
            .add(Component.create(
                    R.string.main_about_openssl_title,
                    "1.0.2o",
                    R.string.main_about_openssl_license_title,
                    R.string.main_about_openssl_license_url))
            .add(Component.create(
                    R.string.main_about_libxml_title,
                    "2-2.9.8",
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_podofo_title,
                    "0.9.4",
                    R.string.main_about_lgpl_license_title,
                    R.string.main_about_lgpl_license_url))
            .add(Component.create(
                    R.string.main_about_z_lib_title,
                    "1.2.11",
                    R.string.main_about_z_lib_license_title,
                    R.string.main_about_z_lib_license_url))
            .add(Component.create(
                    R.string.main_about_support_preference_v7_fix_title,
                    BuildConfig.PREFERENCE_V7_FIX_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_guava_title,
                    BuildConfig.GUAVA_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_square_okio_title,
                    BuildConfig.OKIO_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_timber_title,
                    BuildConfig.TIMBER_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_square_okhttp_title,
                    BuildConfig.OK_HTTP_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_retrofit_title,
                    BuildConfig.RETROFIT_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_bouncy_castle_title,
                    BuildConfig.BOUNCY_CASTLE_VERSION,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_material_values_title,
                    BuildConfig.MATERIAL_VALUES_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_dagger_title,
                    BuildConfig.DAGGER_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_conductor_title,
                    BuildConfig.CONDUCTOR_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_rxjava_title,
                    BuildConfig.RX_JAVA_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_rxandroid_title,
                    BuildConfig.RX_ANDROID_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_rxbinding_title,
                    BuildConfig.RX_BINDING_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_autovalue_title,
                    BuildConfig.AUTO_VALUE_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_autovalue_parcel_title,
                    BuildConfig.AUTO_VALUE_PARCEL_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_threetenabp_title,
                    BuildConfig.THREE_TEN_ABP_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_expandable_layout_title,
                    BuildConfig.EXPANDABLE_LAYOUT_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_ldap_sdk_title,
                    BuildConfig.LDAP_SDK_VERSION,
                    R.string.main_about_lgpl_2_1_license_title,
                    R.string.main_about_lgpl_2_1_license_url))
            .add(Component.create(
                    R.string.main_about_cdoc4j_title,
                    BuildConfig.CDOC4J_VERSION,
                    R.string.main_about_lgpl_2_1_license_title,
                    R.string.main_about_lgpl_2_1_license_url))
            .add(Component.create(
                    R.string.main_about_slf4j_title,
                    BuildConfig.SLF4J_VERSION,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_junit_title,
                    BuildConfig.JUNIT_VERSION,
                    R.string.main_about_eclipse_license_title,
                    R.string.main_about_eclipse_license_url))
            .add(Component.create(
                    R.string.main_about_truth_title,
                    BuildConfig.TRUTH_VERSION,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_mockito_title,
                    BuildConfig.MOCKITO_VERSION,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_acs_title,
                    "1.1.4",
                    R.string.main_about_terms_and_conditions,
                    R.string.main_about_acs_license_url))
            .add(Component.create(
                    R.string.main_about_identiv_title,
                    "1.2",
                    R.string.main_about_terms_and_conditions,
                    R.string.main_about_identiv_license_url))
            .build();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        switch (viewType) {
            case R.layout.main_about_header:
                return new HeaderViewHolder(itemView);
            case R.layout.main_about_component:
                return new ComponentViewHolder(itemView);
            default:
                throw new IllegalArgumentException("Unknown view type " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Resources resources = holder.itemView.getResources();
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).riaDigiDocVersionTitle.setText(resources.getString(
                    R.string.main_about_ria_digidoc_version_title,
                    BuildConfig.VERSION_NAME));
        } else if (holder instanceof ComponentViewHolder) {
            Component component = components.get(position - 1);
            ComponentViewHolder componentViewHolder = (ComponentViewHolder) holder;
            componentViewHolder.nameView.setText(resources.getString(
                    component.name(), component.version()));
            componentViewHolder.licenseNameView.setText(component.licenseName());
            componentViewHolder.licenseUrlView.setText(component.licenseUrl());
        }
    }

    @Override
    public int getItemCount() {
        return components.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return R.layout.main_about_header;
        } else {
            return R.layout.main_about_component;
        }
    }

    static final class HeaderViewHolder extends RecyclerView.ViewHolder {

        final TextView riaDigiDocVersionTitle;

        HeaderViewHolder(View itemView) {
            super(itemView);
            riaDigiDocVersionTitle = itemView.findViewById(R.id.mainAboutRiaDigiDocVersionTitle);
        }
    }

    static final class ComponentViewHolder extends RecyclerView.ViewHolder {

        final TextView nameView;
        final TextView licenseNameView;
        final TextView licenseUrlView;

        ComponentViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.mainAboutComponentName);
            licenseNameView = itemView.findViewById(R.id.mainAboutComponentLicenseName);
            licenseUrlView = itemView.findViewById(R.id.mainAboutComponentLicenseUrl);
        }
    }

    @AutoValue
    static abstract class Component {

        @StringRes abstract int name();

        abstract String version();

        @StringRes abstract int licenseName();

        @StringRes abstract int licenseUrl();

        static Component create(@StringRes int name, String version, @StringRes int licenseName,
                                @StringRes int licenseUrl) {
            return new AutoValue_AboutAdapter_Component(name, version, licenseName, licenseUrl);
        }
    }
}
