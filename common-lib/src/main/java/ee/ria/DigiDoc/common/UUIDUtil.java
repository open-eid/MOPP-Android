package ee.ria.DigiDoc.common;

public class UUIDUtil {

    private static final String UUID_REGEX = "/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/";

    private UUIDUtil() {
    }

    public static boolean isValid(String stringRepresentation) {
        return stringRepresentation != null && stringRepresentation.matches(UUID_REGEX);
    }

}
