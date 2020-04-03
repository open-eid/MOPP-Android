package ee.ria.DigiDoc.sign.utils;

import android.content.Context;
import android.util.Patterns;

import java.util.regex.Matcher;

public class ErrorMessageWithURL {

    public String messageWithURL(Context context, String errorMessage, int urlTranslation, int messageTranslation) {
        if (!extractLink(errorMessage).isEmpty()) {
            return "<span>" +
                    removeLink(errorMessage) + "</span> <a href=" +
                    extractLink(getTextFromTranslation(context, urlTranslation)) + ">" +
                    getTextFromTranslation(context, messageTranslation) + "</a>";
        }

        return errorMessage;
    }

    private String getTextFromTranslation(Context context, int textId) {
        return context.getResources().getString(textId);
    }

    private String extractLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return m.group();
        }

        return "";
    }

    private String removeLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return text.replace(m.group(),"");
        }

        return text;
    }

}
