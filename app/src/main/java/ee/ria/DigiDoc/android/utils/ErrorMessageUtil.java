package ee.ria.DigiDoc.android.utils;

import android.util.Patterns;

import java.util.regex.Matcher;

public final class ErrorMessageUtil {
    public static String extractLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return m.group();
        }

        return "";
    }

    public static String removeLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return text.replace(m.group(),"");
        }

        return text;
    }
}
