package ee.ria.DigiDoc.android.signature.detail;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.common.base.Splitter;

import org.apache.commons.text.WordUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import timber.log.Timber;

@SuppressLint("ViewConstructor")
public final class CertificateDetailView extends CoordinatorLayout {

    private final Navigator navigator;
    private final Toolbar toolbarView;

    private final ViewDisposables disposables = new ViewDisposables();

    public CertificateDetailView(Context context, X509Certificate certificate) {
        super(context);

        inflate(context, R.layout.certificate_details_screen, this);
        AccessibilityUtils.setViewAccessibilityPaneTitle(this, R.string.certificate_details_title);

        navigator = ApplicationApp.component(context).navigator();
        toolbarView = findViewById(R.id.toolbar);

        toolbarView.setTitle(R.string.certificate_details_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        setData(certificate);
    }


    private void setData(X509Certificate certificate) {
        JcaX509CertificateHolder certificateHolder = certificateToJcaX509(certificate);

        if (certificateHolder != null) {
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.C), findViewById(R.id.certificateDetailCountry));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.O), findViewById(R.id.certificateDetailOrganization));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.OU), findViewById(R.id.certificateDetailOrganizationalUnit));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.CN).replace("\\,", ", "), findViewById(R.id.certificateDetailCommonName));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.SURNAME), findViewById(R.id.certificateDetailSurname));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.GIVENNAME), findViewById(R.id.certificateDetailGivenName));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getSubject(), BCStyle.SERIALNUMBER), findViewById(R.id.certificateDetailSerialCode));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getIssuer(), BCStyle.C), findViewById(R.id.certificateDetailIssuerCountry));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getIssuer(), BCStyle.O), findViewById(R.id.certificateDetailIssuerOrganization));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getIssuer(), BCStyle.CN), findViewById(R.id.certificateDetailIssuerCommonName));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getIssuer(), BCStyle.EmailAddress), findViewById(R.id.certificateDetailIssuerEmail));
            TextUtil.handleDetailText(getRDNValue(certificateHolder.getIssuer(), BCStyle.ORGANIZATION_IDENTIFIER), findViewById(R.id.certificateDetailIssuerOrganizationIdentifier));
            TextUtil.handleDetailText(addLeadingZeroToHex(formatHexString(certificate.getSerialNumber().toString(16))), findViewById(R.id.certificateDetailSerialNumber));
            TextUtil.handleDetailText(String.valueOf(certificate.getVersion()), findViewById(R.id.certificateDetailVersion));
            TextUtil.handleDetailText(certificate.getSigAlgName() + " (" + certificate.getSigAlgOID() + ")", findViewById(R.id.certificateDetailSignatureAlgorithm));
            TextUtil.handleDetailText(certificate.getSigAlgParams() == null || new String(certificate.getSigAlgParams(), StandardCharsets.UTF_8).trim().isEmpty() ? "None" :
                    new String(certificate.getSigAlgParams(), StandardCharsets.UTF_8), findViewById(R.id.certificateDetailSignatureParameters));
            TextUtil.handleDetailText(dateToCertificateFormat(certificate.getNotBefore()), findViewById(R.id.certificateDetailNotValidBefore));
            TextUtil.handleDetailText(dateToCertificateFormat(certificate.getNotAfter()), findViewById(R.id.certificateDetailNotValidAfter));
            TextUtil.handleDetailText(certificate.getPublicKey().getAlgorithm(), findViewById(R.id.certificateDetailPublicKeyAlgorithm));
            ASN1Encodable publicKeyParameters = certificateHolder.getSubjectPublicKeyInfo().getAlgorithm().getParameters();
            TextUtil.handleDetailText(!publicKeyParameters.toString().equals("NULL") ? publicKeyParameters.toString() : "None", findViewById(R.id.certificateDetailPublicKeyParameters));
            TextUtil.handleDetailText(getPublicKeyString(certificate.getPublicKey()), findViewById(R.id.certificateDetailPublicKeyPK));
            TextUtil.handleDetailText(getKeyUsages(certificate.getKeyUsage()), findViewById(R.id.certificateDetailPublicKeyKeyUsage));
            TextUtil.handleDetailText(formatHexString(Hex.toHexString(certificate.getSignature())), findViewById(R.id.certificateDetailSignature));
            TextUtil.handleDetailText(getExtensionsData(certificateHolder, certificate), findViewById(R.id.certificateDetailExtensions));
            TextUtil.handleDetailText(getCertificateSHA256Fingerprint(certificate), findViewById(R.id.certificateDetailFingerprintsSHA256));
            TextUtil.handleDetailText(getCertificateSHA1Fingerprint(certificate), findViewById(R.id.certificateDetailFingerprintsSHA1));
        }
    }

    private static List<String> arrayFromString(String arrayString) {
        String[] listOfStrings = arrayString.replace("[", "").replace("]", "").split(", ");
        return Arrays.asList(listOfStrings);
    }

    private static String getExtensionsData(JcaX509CertificateHolder certificateHolder, X509Certificate certificate) {
        StringBuilder stringBuilder = new StringBuilder();

        for (ASN1ObjectIdentifier oid : certificateHolder.getExtensions().getExtensionOIDs()) {
            stringBuilder
                    .append("Extension \n")
                    .append(getExtensionName(certificateHolder.getExtensions().getExtension(oid)))
                    .append(" ( ").append(oid.toString()).append(" )")
                    .append("\n\n");
            stringBuilder.append(indentText("Critical: \n"))
                    .append(indentText(" "))
                    .append(certificateHolder.getExtensions().getExtension(oid).isCritical())
                    .append("\n");

            try {
                ASN1Primitive extensionValue = JcaX509ExtensionUtils.parseExtensionValue(certificate.getExtensionValue(oid.getId()));
                List<String> eVs = arrayFromString(extensionValue.toString());
                for (String ext : eVs) {
                    stringBuilder.append(indentText(" \n"))
                            .append(getIDOrURIString(ext))
                            .append("\n\n");
                }
            } catch (IOException e) {
                Timber.log(Log.ERROR, e, "Unable to parse extension value");
            }
        }

        return stringBuilder.toString();
    }

    private static String getIDOrURIString(String extensionValue) {
        if (URLUtil.isValidUrl(extensionValue)) {
            return indentText("URI: \n") + indentText(WordUtils.wrap(extensionValue, 40,
                    "\n" + indentText(""), true));
        } else if (extensionValue.contains("#")) {
            String[] extracted = extensionValue.split("#");
            return indentText("ID: \n") + indentText(WordUtils.wrap(formatHexString(extracted[1]), 40,
                    "\n" + indentText(""), true));
        }

        return indentText(extensionValue);
    }

    private static String formatHexString(String text) {
        return TextUtils.join(" ",
                Splitter.fixedLength(2).split(text)).trim().toUpperCase();
    }

    private static String getExtensionName(Extension extension) {
        for (Map.Entry<String, String> mapEntry : getExtensionFields().entrySet()) {
            if (mapEntry.getValue().equals(extension.getExtnId().getId())) {
                return mapEntry.getKey();
            }
        }

        return "";
    }

    private static Map<String, String> getExtensionFields() {
        Map<String,String> extensionFields = new HashMap<>();

        extensionFields.put("subjectDirectoryAttributes", "2.5.29.9");
        extensionFields.put("subjectKeyIdentifier", "2.5.29.14");
        extensionFields.put("keyUsage", "2.5.29.15");
        extensionFields.put("privateKeyUsagePeriod", "2.5.29.16");
        extensionFields.put("subjectAlternativeName", "2.5.29.17");
        extensionFields.put("issuerAlternativeName", "2.5.29.18");
        extensionFields.put("basicConstraints", "2.5.29.19");
        extensionFields.put("cRLNumber", "2.5.29.20");
        extensionFields.put("reasonCode", "2.5.29.21");
        extensionFields.put("instructionCode", "2.5.29.23");
        extensionFields.put("invalidityDate", "2.5.29.24");
        extensionFields.put("deltaCRLIndicator", "2.5.29.27");
        extensionFields.put("issuingDistributionPoint", "2.5.29.28");
        extensionFields.put("certificateIssuer", "2.5.29.29");
        extensionFields.put("nameConstraints", "2.5.29.30");
        extensionFields.put("cRLDistributionPoints", "2.5.29.31");
        extensionFields.put("certificatePolicies", "2.5.29.32");
        extensionFields.put("policyMappings", "2.5.29.33");
        extensionFields.put("authorityKeyIdentifier", "2.5.29.35");
        extensionFields.put("policyConstraints", "2.5.29.36");
        extensionFields.put("extendedKeyUsage", "2.5.29.37");
        extensionFields.put("freshestCRL", "2.5.29.46");
        extensionFields.put("inhibitAnyPolicy", "2.5.29.54");
        extensionFields.put("authorityInfoAccess", "1.3.6.1.5.5.7.1.1");
        extensionFields.put("subjectInfoAccess", "1.3.6.1.5.5.7.1.11");
        extensionFields.put("logoType", "1.3.6.1.5.5.7.1.12");
        extensionFields.put("biometricInfo", "1.3.6.1.5.5.7.1.2");
        extensionFields.put("qCStatements", "1.3.6.1.5.5.7.1.3");
        extensionFields.put("auditIdentity", "1.3.6.1.5.5.7.1.4");
        extensionFields.put("noRevAvail", "2.5.29.56");
        extensionFields.put("targetInformation", "2.5.29.55");
        extensionFields.put("expiredCertsOnCRL", "2.5.29.60");

        return extensionFields;

    }

    private static String getCertificateSHA256Fingerprint(X509Certificate certificate) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return formatHexString(Hex.toHexString(sha256.digest(certificate.getEncoded())));
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            Timber.log(Log.ERROR, e, "Unable to get SHA256 digest");
            return "";
        }
    }

    private static String getCertificateSHA1Fingerprint(X509Certificate certificate) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return formatHexString(Hex.toHexString(sha1.digest(certificate.getEncoded())));
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            Timber.log(Log.ERROR, e, "Unable to get SHA1 digest");
            return "";
        }
    }

    private static String getPublicKeyString(PublicKey pk) {
        if (pk instanceof RSAPublicKey) {
            BigInteger modulus = ((RSAPublicKey) pk).getModulus();
            return formatHexString(modulus.toString(16));
        } else if (pk instanceof ECPublicKey) {
            return formatHexString((((ECPublicKey) pk).getW().getAffineX().toString(16) +
                    ((ECPublicKey) pk).getW().getAffineY().toString(16)));
        }

        return Hex.toHexString(pk.getEncoded());
    }

    private static String getKeyUsages(boolean[] certificateKeyUsages) {
        ArrayList<String> keyUsages = new ArrayList<>();
        if (certificateKeyUsages != null) {
            for (int i = 0; i < certificateKeyUsages.length; i++) {
                if (certificateKeyUsages[i]) {
                    keyUsages.add(getKeyUsageDescription(i));
                }
            }

            return TextUtils.join(", ", keyUsages);
        }

        return "";
    }

    private static String getKeyUsageDescription(int keyUsageNum) {
        switch (keyUsageNum) {
            case 0:
                return "Digital Signature";
            case 1:
                return "Non-Repudiation";
            case 2:
                return "Key Encipherment";
            case 3:
                return "Data Encipherment";
            case 4:
                return "Key Agreement";
            case 5:
                return "Key Cert Sign";
            case 6:
                return "cRL Sign";
            case 7:
                return "Encipher Only";
            case 8:
                return "Decipher Only";
            default:
                return "";
        }
    }

    private static String dateToCertificateFormat(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("EEEE, d MMMM yyyy HH:mm:ss Z");
        return dateFormat.format(date);
    }

    private static String getRDNValue(X500Name x500Name, ASN1ObjectIdentifier asn1ObjectIdentifier) {
        if (x500Name.getRDNs(asn1ObjectIdentifier).length > 0) {
            return IETFUtils.valueToString(x500Name.getRDNs(asn1ObjectIdentifier)[0].getFirst().getValue());
        }

        return "";
    }

    private static String addLeadingZeroToHex(String hexString) {
        if (hexString.length() % 2 != 0) {
            return "0" + hexString;
        }

        return hexString;
    }

    private static JcaX509CertificateHolder certificateToJcaX509(X509Certificate certificate) {
        try {
            return new JcaX509CertificateHolder(certificate);
        } catch (CertificateEncodingException e) {
            Timber.log(Log.ERROR, e, "Unable to encode certificate");
            return null;
        }
    }

    private static String indentText(String text) {
        return "\t\t\t\t" + text;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel -> {});
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel -> {}));
    }
}
