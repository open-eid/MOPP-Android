package ee.ria.mopp.androidmobileid.dto;

import com.google.gson.Gson;

import java.util.List;

public class MobileCreateSignatureRequest {

    private String idCode;
    private String country;
    private String phoneNr;
    private String language;
    private String serviceName;
    private String messageToDisplay;
    private String role;
    private String city;
    private String stateOrProvince;
    private String postalCode;
    private String countryName;
    private String signingProfile;
    private String format;
    private String version;
    private String signatureId;
    private String messagingMode;
    private Integer asyncConfiguration;
    private List<DataFileDto> datafiles;

    public static String toJson(MobileCreateSignatureRequest mobileCreateSignatureRequest) {
        return new Gson().toJson(mobileCreateSignatureRequest);
    }

    public static MobileCreateSignatureRequest fromJson(String json) {
        return new Gson().fromJson(json, MobileCreateSignatureRequest.class);
    }

    public String getIdCode() {
        return idCode;
    }

    public void setIdCode(String idCode) {
        this.idCode = idCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhoneNr() {
        return phoneNr;
    }

    public void setPhoneNr(String phoneNr) {
        this.phoneNr = phoneNr;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMessageToDisplay() {
        return messageToDisplay;
    }

    public void setMessageToDisplay(String messageToDisplay) {
        this.messageToDisplay = messageToDisplay;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateOrProvince() {
        return stateOrProvince;
    }

    public void setStateOrProvince(String stateOrProvince) {
        this.stateOrProvince = stateOrProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getSigningProfile() {
        return signingProfile;
    }

    public void setSigningProfile(String signingProfile) {
        this.signingProfile = signingProfile;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }

    public String getMessagingMode() {
        return messagingMode;
    }

    public void setMessagingMode(String messagingMode) {
        this.messagingMode = messagingMode;
    }

    public Integer getAsyncConfiguration() {
        return asyncConfiguration;
    }

    public void setAsyncConfiguration(Integer asyncConfiguration) {
        this.asyncConfiguration = asyncConfiguration;
    }

    public List<DataFileDto> getDatafiles() {
        return datafiles;
    }

    public void setDatafiles(List<DataFileDto> datafiles) {
        this.datafiles = datafiles;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MobileCreateSignatureRequest{");
        sb.append("idCode='").append(idCode).append('\'');
        sb.append(", country='").append(country).append('\'');
        sb.append(", phoneNr='").append(phoneNr).append('\'');
        sb.append(", language='").append(language).append('\'');
        sb.append(", serviceName='").append(serviceName).append('\'');
        sb.append(", messageToDisplay='").append(messageToDisplay).append('\'');
        sb.append(", role='").append(role).append('\'');
        sb.append(", city='").append(city).append('\'');
        sb.append(", stateOrProvince='").append(stateOrProvince).append('\'');
        sb.append(", postalCode='").append(postalCode).append('\'');
        sb.append(", countryName='").append(countryName).append('\'');
        sb.append(", signingProfile='").append(signingProfile).append('\'');
        sb.append(", format='").append(format).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", signatureId='").append(signatureId).append('\'');
        sb.append(", messagingMode='").append(messagingMode).append('\'');
        sb.append(", asyncConfiguration=").append(asyncConfiguration);
        sb.append(", datafiles=").append(datafiles);
        sb.append('}');
        return sb.toString();
    }
}
