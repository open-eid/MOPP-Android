package ee.ria.cryptolib;

import android.support.annotation.WorkerThread;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.threeten.bp.LocalDate;

import java.util.Locale;

import ee.ria.DigiDoc.EIDType;
import timber.log.Timber;

public final class RecipientRepository {

    @WorkerThread
    public final ImmutableList<Recipient> find(String query) throws ConnectivityException {
        ImmutableList.Builder<Recipient> builder = ImmutableList.builder();

        try {
            LdapConnection connection = new LdapNetworkConnection("ldap.sk.ee");
            connection.bind();

            String attr = CharMatcher.inRange('0', '9').matchesAllOf(query) ? "serialNumber" : "cn";

            EntryCursor cursor = connection.search("c=EE",
                    String.format(Locale.US, "(%s=%s)", attr, query), SearchScope.SUBTREE);
            while (cursor.next()) {
                Entry entry = cursor.get();
                Timber.e("ENTRY: %s", entry);

                builder.add(Recipient.create(
                        EIDType.ID_CARD, entry.get("cn").getString(), LocalDate.now()));
            }

            cursor.close();
            connection.unBind();
            connection.close();
        } catch (Exception e) {
            throw new ConnectivityException(e);
        }

        return builder.build();
    }
}
