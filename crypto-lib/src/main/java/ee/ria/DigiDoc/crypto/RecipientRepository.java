package ee.ria.DigiDoc.crypto;

import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.util.LDAPTestUtils;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TLSCipherSuiteSelector;

import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.EIDType;
import ee.ria.DigiDoc.common.TrustManagerUtil;
import okio.ByteString;

import static com.unboundid.ldap.sdk.SearchScope.SUB;

/**
 * Repository for encryption recipients from SK LDAP and ESTEID SK LDAP server.
 */
public final class RecipientRepository {

    private static final int LDAP_PORT = 636;
    private static final String CERT_BINARY_ATTR = "userCertificate;binary";
    private static final String BASE_DN = "c=EE";

    private final String ldapPersonServiceUrl;
    private final String ldapCorpServiceUrl;

    public RecipientRepository(String ldapPersonServiceUrl, String ldapCorpServiceUrl) {
        // Removing protocols from URL-s
        this.ldapPersonServiceUrl = ldapPersonServiceUrl.split("://")[1];
        this.ldapCorpServiceUrl = ldapCorpServiceUrl.split("://")[1];
    }

    /**
     * Tries to find certificates first from ESTEID SK LDAP server. If that fails or no certificates
     * are found then tries to find certificates from SK LDAP Server
     * <p>
     * If query is numeric then searches by personal code, otherwise by CN fields.
     *
     * @param query Query to executeSearch for.
     * @return List of matched certificates.
     * @throws CryptoException When something went wrong
     */
    @WorkerThread
    public final ImmutableList<Certificate> find(String query) throws CryptoException {
        ImmutableList<Certificate> certs;
        try {
            certs = findPersonCertificate(query);
        } catch (NoInternetConnectionException e) {
            throw e;
        } catch (CryptoException e) {
            return findCorporationCertificate(query);
        }
        return certs.isEmpty() ? findCorporationCertificate(query) : certs;
    }

    private ImmutableList<Certificate> findPersonCertificate(String query) throws CryptoException {
        return search(ldapPersonServiceUrl, new EstEidLdapFilter(query));
    }

    private ImmutableList<Certificate> findCorporationCertificate(String query) throws CryptoException {
        return search(ldapCorpServiceUrl, new LdapFilter(query));
    }

    private ImmutableList<Certificate> search(String url, LdapFilter ldapFilter) throws CryptoException {
        try (LDAPConnection connection = new LDAPConnection(getDefaultKeystoreSslSocketFactory())) {
            connection.connect(url, LDAP_PORT);
            return executeSearch(connection, ldapFilter);
        } catch (Exception e) {
            if (e instanceof LDAPException && ((LDAPException) e).getResultCode().equals(ResultCode.CONNECT_ERROR)) {
                throw new NoInternetConnectionException();
            }
            throw new CryptoException("Finding recipients failed", e);
        }
    }

    private ImmutableList<Certificate> executeSearch(LDAPConnection connection, LdapFilter ldapFilter)
            throws LDAPException, IOException {

        int maximumNumberOfResults = 50;
        SearchRequest searchRequest = new SearchRequest(BASE_DN, SUB, ldapFilter.filterString(), CERT_BINARY_ATTR);
        ASN1OctetString extraResponseCookie = null;
        ImmutableList.Builder<Certificate> builder = ImmutableList.builder();
        while (true) {
            searchRequest.setControls(new SimplePagedResultsControl(maximumNumberOfResults, extraResponseCookie));
            SearchResult searchResult = connection.search(searchRequest);
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                for (Attribute attribute : entry.getAttributes()) {
                    for (ASN1OctetString value : attribute.getRawValues()) {
                        Certificate certificate = Certificate.create(ByteString.of(value.getValue()));
                        if (isSuitableKeyAndNotMobileId(certificate)) {
                            builder.add(certificate);
                        }
                    }
                }
            }

            LDAPTestUtils.assertHasControl(searchResult, SimplePagedResultsControl.PAGED_RESULTS_OID);
            SimplePagedResultsControl releaseControl = SimplePagedResultsControl.get(searchResult);
            if (releaseControl != null && releaseControl.moreResultsToReturn() && searchResult.getEntryCount() < maximumNumberOfResults) {
                extraResponseCookie = releaseControl.getCookie();
            } else {
                break;
            }
        }

        return builder.build();
    }

    private SSLSocketFactory getDefaultKeystoreSslSocketFactory() throws GeneralSecurityException, IOException {
        TLSCipherSuiteSelector.setAllowSHA1(true);
        TLSCipherSuiteSelector.setAllowRSAKeyExchange(true);
        return TrustManagerUtil.createDefaultKeystoreSSLUtil().createSSLSocketFactory();
    }

    private boolean isSuitableKeyAndNotMobileId(Certificate certificate) {
        return (hasKeyEnciphermentUsage(certificate) || hasKeyAgreementUsage(certificate)) &&
                !isServerAuthKeyPurpose(certificate) &&
                (!isESealType(certificate) || !isTlsClientAuthKeyPurpose(certificate)) &&
                !isMobileIdType(certificate);
    }

    private boolean isTlsClientAuthKeyPurpose(Certificate certificate) {
        return certificate.extendedKeyUsage().hasKeyPurposeId(KeyPurposeId.id_kp_clientAuth);
    }

    private boolean hasKeyAgreementUsage(Certificate certificate) {
        return certificate.keyUsage().hasUsages(KeyUsage.keyAgreement);
    }

    private boolean hasKeyEnciphermentUsage(Certificate certificate) {
        return certificate.keyUsage().hasUsages(KeyUsage.keyEncipherment);
    }

    private boolean isServerAuthKeyPurpose(Certificate certificate) {
        return certificate.extendedKeyUsage().hasKeyPurposeId(KeyPurposeId.id_kp_serverAuth);
    }

    private boolean isMobileIdType(Certificate certificate) {
        return certificate.type().equals(EIDType.MOBILE_ID);
    }

    private boolean isESealType(Certificate certificate) {
        return certificate.type().equals(EIDType.E_SEAL);
    }
}
