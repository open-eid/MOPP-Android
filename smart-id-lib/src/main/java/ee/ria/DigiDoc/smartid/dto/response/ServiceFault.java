package ee.ria.DigiDoc.smartid.dto.response;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

public class ServiceFault {

    private final SessionStatusResponse.ProcessStatus status;
    @Nullable private final String detailMessage;

    public ServiceFault(SessionStatusResponse.ProcessStatus status, @Nullable String detailMessage) {
        this.status = status;
        this.detailMessage = detailMessage;
    }

    public static String toJson(ServiceFault fault) {
        return new Gson().toJson(fault);
    }

    public static ServiceFault fromJson(String json) {
        return new Gson().fromJson(json, ServiceFault.class);
    }

    public SessionStatusResponse.ProcessStatus getStatus() {
        return status;
    }

    @Nullable public String getDetailMessage() {
        return detailMessage;
    }
}
