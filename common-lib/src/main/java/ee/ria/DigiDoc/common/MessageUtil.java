package ee.ria.DigiDoc.common;

import com.google.gson.Gson;

import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.Charset;

import threegpp.charset.gsm.GSMCharset;
import threegpp.charset.ucs2.UCS2Charset80;

public class MessageUtil {

    public static final Charset GSM_CHARSET = new GSMCharset();
    public static final Charset UCS2_CHARSET = new UCS2Charset80();

    public static String trimDisplayMessageIfNotWithinSizeLimit(String displayMessage, int maxDisplayMessageBytes, Charset charset) {
        byte[] displayMessagesBytes = displayMessage.getBytes(charset);
        if (displayMessagesBytes.length > maxDisplayMessageBytes) {
            double bytesPerChar = (double) displayMessagesBytes.length / (double) displayMessage.length();
            return displayMessage.substring(0, (int) ((maxDisplayMessageBytes - 4) / bytesPerChar)) + "...";
        }
        return displayMessage;
    }

    public static String escape(String text) {
        return StringEscapeUtils.escapeJava(text);
    }

    public static String unEscape(String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    public static String toJsonString(Object object) {
        Gson gson = new Gson().newBuilder().disableHtmlEscaping().disableInnerClassSerialization().create();
        return MessageUtil.unEscape(gson.toJson(object));
    }
}
