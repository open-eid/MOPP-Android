package ee.ria.DigiDoc.common;

import java.nio.charset.Charset;

import threegpp.charset.gsm.GSMCharset;


public class MessageUtil {

    public static String trimDisplayMessageIfNotWithinSizeLimit(String displayMessage, int maxDisplayMessageBytes, Charset charset) {
        byte[] displayMessagesBytes = displayMessage.getBytes(charset);
        if (displayMessagesBytes.length > maxDisplayMessageBytes) {
            double bytesPerChar = (double) displayMessagesBytes.length / (double) displayMessage.length();
            return displayMessage.substring(0, (int) ((maxDisplayMessageBytes - 4) / bytesPerChar)) + "...";
        }
        return displayMessage;
    }

    public static Charset getGSM7Charset() {
        return new GSMCharset();
    }
}
