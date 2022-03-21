package ee.ria.DigiDoc.common;

import com.google.auto.value.AutoValue;

import java.io.Serializable;
import java.util.List;

@AutoValue
public abstract class RoleData implements Serializable {

    public abstract List<String> roles();

    public abstract String city();

    public abstract String state();

    public abstract String country();

    public abstract String zip();

    public static RoleData create(List<String> roles, String city, String state, String country, String zip) {
        return new AutoValue_RoleData(roles, city, state, country, zip);
    }
}
