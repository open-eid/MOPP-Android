package ee.ria.DigiDoc.crypto;

import com.google.common.base.CharMatcher;

import java.util.Locale;

public class LdapFilter {

    private final String query;
    private final boolean serialNumberSearch;

    LdapFilter(String query) {
        this.query = query;
        this.serialNumberSearch = CharMatcher.inRange('0', '9').matchesAllOf(query);
    }

    boolean isSerialNumberSearch() {
        return serialNumberSearch;
    }

    String getQuery() {
        return query;
    }

    String filterString() {
        if (isSerialNumberSearch()) {
            return String.format(Locale.US, "(serialNumber=%s)", query);
        } else {
            return String.format(Locale.US, "(cn=%s)", query);
        }
    }

}
