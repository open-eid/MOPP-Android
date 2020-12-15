package ee.ria.DigiDoc.sign.utils;

import android.content.Context;
import android.util.Patterns;

import java.util.regex.Matcher;

public class ErrorMessage {

    public static String withURL(Context context, int errorMessageTranslation, int urlMessageTranslation) {
        String errorMessage = getTextFromTranslation(context, errorMessageTranslation);
        String link = extractLink(errorMessage);
        if (!link.isEmpty()) {
            return "<span>" +
                    removeLink(errorMessage) + "</span> <a href=" + link + ">" +
                    getTextFromTranslation(context, urlMessageTranslation) + "</a>";
        }
        return errorMessage;
    }

    private static String getTextFromTranslation(Context context, int textId) {
        return context.getResources().getString(textId);
    }

    private static String extractLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return m.group();
        }

        return "";
    }

    private static String removeLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return text.replace(m.group(),"");
        }

        return text;
    }

}
