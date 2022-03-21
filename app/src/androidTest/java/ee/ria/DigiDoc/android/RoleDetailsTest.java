package ee.ria.DigiDoc.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.io.ByteStreams;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.libdigidocpp.StringVector;

public class RoleDetailsTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private static final String CONTAINER_FILENAME = "example.asice";

    @Test
    public void container_is_not_missing() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        assertNotNull(signedContainer);
    }

    @Test
    public void container_has_signatures() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        assertNotNull(signedContainer.signatures());
        assertTrue(signedContainer.signatures().size() > 0);
    }

    @Test
    public void signature_has_role_and_address_data() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        Signature signature = signedContainer.signatures().get(0);
        assertNotNull(signature.roles());
        assertNotNull(signature.city());
        assertNotNull(signature.state());
        assertNotNull(signature.country());
        assertNotNull(signature.zip());
    }

    @Test
    public void signature_role_and_address_data_correct_for_signature_with_role() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        Signature signature = signedContainer.signatures().get(0);
        List<String> roles = new ArrayList<>(Arrays.asList("Roll"));
        assertEquals(roles, signature.roles());
        assertEquals("Linn", signature.city());
        assertEquals("Maakond", signature.state());
        assertEquals("EE", signature.country());
        assertEquals("12345", signature.zip());
    }

    @Test
    public void signature_role_and_address_data_not_incorrect_for_signature_with_role() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        Signature signature = signedContainer.signatures().get(0);
        List<String> roles = new ArrayList<>(Arrays.asList("No role"));
        assertNotEquals(roles, signature.roles());
        assertNotEquals("No City", signature.city());
        assertNotEquals("No State", signature.state());
        assertNotEquals("No Country", signature.country());
        assertNotEquals("No 12345", signature.zip());
    }

    @Test
    public void signature_role_and_address_data_correct_for_signature_without_role() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        Signature signature = signedContainer.signatures().get(1);
        StringVector emptyRoles = new StringVector();
        assertEquals(emptyRoles, signature.roles());
        assertEquals("", signature.city());
        assertEquals("", signature.state());
        assertEquals("", signature.country());
        assertEquals("", signature.zip());
    }

    @Test
    public void signature_role_and_address_data_not_incorrect_for_signature_without_role() {
        SignedContainer signedContainer = getSignedContainerFromPath(CONTAINER_FILENAME);
        Signature signature = signedContainer.signatures().get(1);
        List<String> roles = new ArrayList<>(Arrays.asList("No role"));
        assertNotEquals(roles, signature.roles());
        assertNotEquals("No City", signature.city());
        assertNotEquals("No State", signature.state());
        assertNotEquals("No country", signature.country());
        assertNotEquals("No 12345", signature.zip());
    }

    private SignedContainer getSignedContainerFromPath(String name) {
        AssetManager assetManager = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
        try {
            for (String fileName : assetManager.list("")) {
                if (fileName.equals(name)) {
                    File assetFile = folder.newFile();
                    try (InputStream containerInputStream = assetManager.open(fileName);
                         OutputStream containerOutputStream = new FileOutputStream(assetFile)) {
                        ByteStreams.copy(containerInputStream, containerOutputStream);
                    }
                    return SignedContainer.open(assetFile);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }
}
