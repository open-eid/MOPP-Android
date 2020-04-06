package ee.ria.DigiDoc.mobileid.dto.response;

import com.google.gson.Gson;

import ee.ria.libdigidocpp.Container;

public class MobileIdResponse {

    private MobileCreateSignatureSessionStatusResponse.ProcessStatus status;
    private Container container;
    private String signature;

    public static String toJson(MobileIdResponse response) {
        return new Gson().toJson(response);
    }

    public static MobileIdResponse fromJson(String json) {
        return new Gson().fromJson(json, MobileIdResponse.class);
    }

    public MobileCreateSignatureSessionStatusResponse.ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(MobileCreateSignatureSessionStatusResponse.ProcessStatus status) {
        this.status = status;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
