package ee.ria.DigiDoc.android.main.about;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.common.TextUtil;

final class AboutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ContentView {

    private final ImmutableList<Component> components = ImmutableList.<Component>builder()
            .add(Component.create(
                    R.string.main_about_libdigidocpp_title,
                    R.string.main_about_lgpl_2_1_license_title,
                    R.string.main_about_lgpl_2_1_license_url))
            .add(Component.create(
                    R.string.main_about_xerces_c_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_xalan_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_apache_xml_security_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_xsd_title,
                    R.string.main_about_gpl_license_title,
                    R.string.main_about_xsd_license_url))
            .add(Component.create(
                    R.string.main_about_openssl_title,
                    R.string.main_about_openssl_license_title,
                    R.string.main_about_openssl_license_url))
            .add(Component.create(
                    R.string.main_about_z_lib_title,
                    R.string.main_about_z_lib_license_title,
                    R.string.main_about_z_lib_license_url))
            .add(Component.create(
                    R.string.main_about_support_preference_v7_fix_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_guava_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_square_okio_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_timber_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_square_okhttp_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_retrofit_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_bouncy_castle_title,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_material_values_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_dagger_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_conductor_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_rxjava_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_rxandroid_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_rxbinding_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_autovalue_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_autovalue_parcel_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_expandable_layout_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_ldap_sdk_title,
                    R.string.main_about_lgpl_2_1_license_title,
                    R.string.main_about_lgpl_2_1_license_url))
            .add(Component.create(
                    R.string.main_about_cdoc4j_title,
                    R.string.main_about_lgpl_2_1_license_title,
                    R.string.main_about_lgpl_2_1_license_url))
            .add(Component.create(
                    R.string.main_about_slf4j_title,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_junit_title,
                    R.string.main_about_eclipse_license_title,
                    R.string.main_about_eclipse_license_url))
            .add(Component.create(
                    R.string.main_about_truth_title,
                    R.string.main_about_apache_2_license_title,
                    R.string.main_about_apache_2_license_url))
            .add(Component.create(
                    R.string.main_about_mockito_title,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mit_license_url))
            .add(Component.create(
                    R.string.main_about_acs_title,
                    R.string.main_about_terms_and_conditions,
                    R.string.main_about_acs_license_url))
            .add(Component.create(
                    R.string.main_about_identiv_title,
                    R.string.main_about_terms_and_conditions,
                    R.string.main_about_identiv_license_url))
            .add(Component.create(
                    R.string.main_about_jackson_core_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_jackson_core_license_url
            ))
            .add(Component.create(
                    R.string.main_about_jackson_databind_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_jackson_databind_license_url
            ))
            .add(Component.create(
                    R.string.main_about_mid_rest_api_java_title,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_mid_rest_api_java_license_url
            ))
            .add(Component.create(
                    R.string.main_about_commons_io_title,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_commons_io_license_url
            ))
            .add(Component.create(
                    R.string.main_about_telecom_charsets_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_telecom_charsets_license_url
            ))
            .add(Component.create(
                    R.string.main_about_project_lombok_title,
                    R.string.main_about_mit_license_title,
                    R.string.main_about_project_lombok_license_url
            ))
            .add(Component.create(
                    R.string.main_about_logback_android_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_logback_android_license_url
            ))
            .add(Component.create(
                    R.string.main_about_localbroadcast_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_androidx_espresso_core_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_androidx_rules_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_androidx_junit_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_androidx_orchestrator_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_androidx_core_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_firebase_crashlytics_gradle_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
            .add(Component.create(
                    R.string.main_about_gson_title,
                    R.string.main_about_apache_2_0_license_title,
                    R.string.main_about_apache_2_license_txt_url
            ))
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
            ((HeaderViewHolder) holder).riaDigiDocVersionTitle.setContentDescription(resources.getString(
                    R.string.main_about_ria_digidoc_version_title,
                    TextUtil.splitTextAndJoin(BuildConfig.VERSION_NAME, " ", " ")));
        } else if (holder instanceof ComponentViewHolder) {
            Component component = components.get(position - 1);
            ComponentViewHolder componentViewHolder = (ComponentViewHolder) holder;
            componentViewHolder.nameView.setText(resources.getString(
                    component.name()));
            componentViewHolder.nameView.setContentDescription(getAccessibilityText(
                    resources.getString(component.name())));
            componentViewHolder.licenseNameView.setText(component.licenseName());
            componentViewHolder.licenseNameView.setContentDescription(getAccessibilityText(
                    resources.getString(component.licenseName())));
            componentViewHolder.licenseUrlView.setText(component.licenseUrl());
            componentViewHolder.licenseUrlView.setContentDescription(getAccessibilityText(
                    resources.getString(component.licenseUrl())) + " link");

            // Navigate to toolbar when previous element is selected on first element
            Component firstComponent = components.get(0);
            if (component == firstComponent) {
                componentViewHolder.licenseUrlView.setNextFocusUpId(R.id.appBar);
            }

            // Navigate to toolbar when next element is selected on last element
            Component lastComponent = components.get(components.size() - 1);
            if (component == lastComponent) {
                componentViewHolder.licenseUrlView.setNextFocusDownId(R.id.appBar);
            }
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

    private StringBuilder getAccessibilityText(String text) {
        StringBuilder nameViewAccessibility = new StringBuilder();
        String[] nameTextSplit = text.split("\\.");

        for (String nameText : nameTextSplit) {
            if (TextUtil.isOnlyDigits(nameText)) {
                nameViewAccessibility.append(" .").append(nameText);
            } else {
                nameViewAccessibility.append(nameText);
            }
        }
        return nameViewAccessibility;
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

        @StringRes abstract int licenseName();

        @StringRes abstract int licenseUrl();

        static Component create(@StringRes int name, @StringRes int licenseName,
                                @StringRes int licenseUrl) {
            return new AutoValue_AboutAdapter_Component(name, licenseName, licenseUrl);
        }
    }
}
