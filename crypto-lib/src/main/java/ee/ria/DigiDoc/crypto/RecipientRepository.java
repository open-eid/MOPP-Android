package ee.ria.DigiDoc.crypto;

import android.support.annotation.WorkerThread;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;

import java.util.Locale;

import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.EIDType;
import okio.ByteString;

/**
 * Repository for encryption recipients from SK LDAP server.
 */
public final class RecipientRepository {

    /**
     * Finds certificates from SK LDAP server.
     *
     * If query is numeric then searches by personal code, otherwise by CN fields with wildcards.
     *
     * @param query Query to search for.
     * @return List of matched certificates.
     * @throws CryptoException When something went wrong
     */
    @WorkerThread
    public final ImmutableList<Certificate> find(String query) throws CryptoException {
        ImmutableList.Builder<Certificate> builder = ImmutableList.builder();

        try {
            LdapConnection connection = new LdapNetworkConnection("ldap.sk.ee");
            connection.bind();

            boolean serialNumberSearch = CharMatcher.inRange('0', '9').matchesAllOf(query);
            String filter;
            if (serialNumberSearch) {
                filter = String.format(Locale.US, "(serialNumber=%s)", query);
            } else {
                filter = String.format(Locale.US, "(cn=*%s*)", query);
            }

            EntryCursor cursor = connection.search("c=EE", filter, SearchScope.SUBTREE,
                    "userCertificate;binary");
            while (cursor.next()) {
                Entry entry = cursor.get();
                for (Attribute attribute : entry) {
                    if (attribute.getId().equals("usercertificate;binary")) {
                        for (Value<?> value : attribute) {
                            Certificate certificate = Certificate
                                    .create(ByteString.of(value.getBytes()));
                            if ((certificate.keyUsage().hasUsages(KeyUsage.keyEncipherment) ||
                                    certificate.keyUsage().hasUsages(KeyUsage.keyAgreement)) &&
                                    !certificate.extendedKeyUsage().hasKeyPurposeId(KeyPurposeId
                                            .id_kp_serverAuth) &&
                                    (serialNumberSearch || !certificate.extendedKeyUsage()
                                            .hasKeyPurposeId(KeyPurposeId.id_kp_clientAuth)) &&
                                    !certificate.type().equals(EIDType.MOBILE_ID)) {
                                builder.add(certificate);
                            }
                        }
                    }
                }
            }

            cursor.close();
            connection.unBind();
            connection.close();
        } catch (Exception e) {
            throw new CryptoException("Finding recipients failed", e);
        }

        return builder.build();
    }
}
