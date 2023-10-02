package ee.ria.DigiDoc.common;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoleData implements Serializable {

    private final List<String> roles;

    private final String city;

    private final String state;

    private final String country;

    private final String zip;

    public RoleData() {
        this.roles = new ArrayList<>();
        this.city = "";
        this.state = "";
        this.country = "";
        this.zip = "";
    }

    public RoleData(List<String> roles, String city, String state, String country, String zip) {
        this.roles = roles;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zip = zip;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getZip() {
        return zip;
    }

    public static RoleData create(List<String> roles, String city, String state, String country, String zip) {
        return new RoleData(roles, city, state, country, zip);
    }

    public static String toJson(RoleData roleData) {
        return new Gson().toJson(roleData);
    }

    @Override
    public String toString() {
        return "RoleData{" +
                "roles='" + String.join(", ", roles) + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", zip='" + zip + '\'' +
                '}';
    }
}
