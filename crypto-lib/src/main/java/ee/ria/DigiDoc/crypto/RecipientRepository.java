package ee.ria.DigiDoc.crypto;

import android.support.annotation.WorkerThread;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

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

        boolean serialNumberSearch = CharMatcher.inRange('0', '9').matchesAllOf(query);
        String filter;
        if (serialNumberSearch) {
            filter = String.format(Locale.US, "(serialNumber=%s)", query);
        } else {
            filter = String.format(Locale.US, "(cn=*%s*)", query);
        }

        try (LDAPConnection connection = new LDAPConnection("ldap.sk.ee", 389)) {
            SearchResult result =
                    connection.search("c=EE", SearchScope.SUB, filter, "userCertificate;binary");
            for (SearchResultEntry entry : result.getSearchEntries()) {
                for (Attribute attribute : entry.getAttributes()) {
                    for (ASN1OctetString value : attribute.getRawValues()) {
                        Certificate certificate =
                                Certificate.create(ByteString.of(value.getValue()));
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
        } catch (Exception e) {
            throw new CryptoException("Finding recipients failed", e);
        }

        return builder.build();
    }
}
