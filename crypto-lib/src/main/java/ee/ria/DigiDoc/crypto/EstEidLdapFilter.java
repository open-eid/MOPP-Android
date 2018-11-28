package ee.ria.DigiDoc.crypto;

import java.util.Locale;

public class EstEidLdapFilter extends LdapFilter {

    EstEidLdapFilter(String query) {
        super(query);
    }

    @Override
    String filterString() {
        if (isSerialNumberSearch()) {
            return String.format(Locale.US, "(serialNumber= PNOEE-%s)", getQuery());
        } else {
            return String.format(Locale.US, "(cn= %s)", getQuery());
        }
    }
}
