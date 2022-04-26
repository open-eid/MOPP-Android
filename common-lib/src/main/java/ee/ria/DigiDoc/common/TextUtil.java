package ee.ria.DigiDoc.common;

public class TextUtil {

    public static String splitTextAndJoin(String text, String delimiter) {
        return text.replaceAll(".(?=.)", "$0" + delimiter);
    }
}
