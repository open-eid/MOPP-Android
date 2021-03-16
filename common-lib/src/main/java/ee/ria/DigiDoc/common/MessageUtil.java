package ee.ria.DigiDoc.common;

import java.nio.charset.StandardCharsets;

public class MessageUtil {

    public static String trimDisplayMessageIfNotWithinSizeLimit(String displayMessage, int maxDisplayMessageBytes, int substringBytes) {
        byte[] displayMessagesBytes = displayMessage.getBytes(StandardCharsets.UTF_8);
        if (displayMessagesBytes.length > maxDisplayMessageBytes) {
            int bytesPerChar = displayMessagesBytes.length / displayMessage.length();
            return displayMessage.substring(0, substringBytes / bytesPerChar) + "...";
        }
        return displayMessage;
    }
}
