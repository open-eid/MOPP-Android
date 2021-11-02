package ee.ria.DigiDoc.configuration;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ConfigurationProvider implements Parcelable {

    public abstract MetaInf getMetaInf();
    public abstract String getConfigUrl();
    public abstract String getSivaUrl();
    public abstract String getTslUrl();
    public abstract List<String> getTslCerts();
    public abstract String getTsaUrl();
    public abstract String getMidSignUrl();
    public abstract String getLdapPersonUrl();
    public abstract String getLdapCorpUrl();
    public abstract String getMidRestUrl();
    public abstract String getMidSkRestUrl();
    public abstract String getSidRestUrl();
    public abstract String getSidSkRestUrl();
    public abstract Map<String, String> getOCSPUrls();
    public abstract List<String> getCertBundle();

    @Nullable
    public abstract Date getConfigurationLastUpdateCheckDate();
    @Nullable
    public abstract Date getConfigurationUpdateDate();

    static ConfigurationProviderBuilder builder() {
        return new AutoValue_ConfigurationProvider.Builder();
    }

    @AutoValue.Builder
    abstract static class ConfigurationProviderBuilder {
        abstract ConfigurationProviderBuilder setMetaInf(MetaInf metaInf);
        abstract ConfigurationProviderBuilder setConfigUrl(String configUrl);
        abstract ConfigurationProviderBuilder setSivaUrl(String sivaUrl);
        abstract ConfigurationProviderBuilder setTslUrl(String tslUrl);
        abstract ConfigurationProviderBuilder setTslCerts(List<String> tslCerts);
        abstract ConfigurationProviderBuilder setTsaUrl(String tsaUrl);
        abstract ConfigurationProviderBuilder setMidSignUrl(String midSignUrl);
        abstract ConfigurationProviderBuilder setLdapPersonUrl(String ldapPersonUrl);
        abstract ConfigurationProviderBuilder setLdapCorpUrl(String ldapCorpUrl);
        abstract ConfigurationProviderBuilder setMidRestUrl(String midRestUrl);
        abstract ConfigurationProviderBuilder setMidSkRestUrl(String midRestUrl);
        abstract ConfigurationProviderBuilder setSidRestUrl(String sidRestUrl);
        abstract ConfigurationProviderBuilder setSidSkRestUrl(String sidRestUrl);
        abstract ConfigurationProviderBuilder setOCSPUrls(Map<String, String> OCSPUrls);
        abstract ConfigurationProviderBuilder setCertBundle(List<String> certBundle);
        abstract ConfigurationProviderBuilder setConfigurationLastUpdateCheckDate(Date lastUpdateCheckDate);
        abstract ConfigurationProviderBuilder setConfigurationUpdateDate(Date updateDate);
        abstract ConfigurationProvider build();
    }

    @AutoValue
    public static abstract class MetaInf implements Parcelable {
        public abstract String getUrl();
        public abstract String getDate();
        public abstract int getSerial();
        public abstract int getVersion();

        static MetaInf.MetaInfBuilder builder() {
            return new AutoValue_ConfigurationProvider_MetaInf.Builder();
        }

        @AutoValue.Builder
        abstract static class MetaInfBuilder {
            abstract MetaInfBuilder setUrl(String url);
            abstract MetaInfBuilder setDate(String date);
            abstract MetaInfBuilder setSerial(int serial);
            abstract MetaInfBuilder setVersion(int version);
            abstract MetaInf build();
        }
    }
}